package com.dispatch.core.server;

import com.dispatch.core.filter.FilterChain;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NettyServer {
    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);
    
    private final int port;
    private final boolean sslEnabled;
    private final FilterChain filterChain;
    private final ExecutorService virtualThreadExecutor;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private SslContext sslContext;
    
    public NettyServer(int port, boolean sslEnabled, FilterChain filterChain) {
        this.port = port;
        this.sslEnabled = sslEnabled;
        this.filterChain = filterChain;
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }
    
    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Starting Dispatch server on port {} (SSL: {})", port, sslEnabled);
                
                if (sslEnabled) {
                    initializeSsl();
                }
                
                bossGroup = new NioEventLoopGroup(1);
                workerGroup = new NioEventLoopGroup();
                
                ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            
                            if (sslContext != null) {
                                pipeline.addLast(sslContext.newHandler(ch.alloc()));
                            }
                            
                            pipeline.addLast(new HttpRequestDecoder());
                            pipeline.addLast(new HttpObjectAggregator(1048576)); // 1MB max request size
                            pipeline.addLast(new HttpResponseEncoder());
                            pipeline.addLast(new DispatchHandler(filterChain, virtualThreadExecutor));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true);
                
                ChannelFuture future = bootstrap.bind(port).sync();
                serverChannel = future.channel();
                
                logger.info("Dispatch server started successfully on port {}", port);
                
                serverChannel.closeFuture().sync();
                
            } catch (Exception e) {
                logger.error("Failed to start server", e);
                throw new RuntimeException("Failed to start server", e);
            } finally {
                shutdown();
            }
        }, virtualThreadExecutor);
    }
    
    private void initializeSsl() throws CertificateException, SSLException {
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        sslContext = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        logger.info("SSL context initialized with self-signed certificate");
    }
    
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            logger.info("Shutting down Dispatch server...");
            
            try {
                if (serverChannel != null) {
                    serverChannel.close().sync();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while closing server channel", e);
            }
            
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
            
            virtualThreadExecutor.shutdown();
            
            logger.info("Dispatch server shutdown complete");
        });
    }
    
    public boolean isRunning() {
        return serverChannel != null && serverChannel.isActive();
    }
    
    public int getPort() {
        return port;
    }
    
    public boolean isSslEnabled() {
        return sslEnabled;
    }
}