package com.dispatch.core.server;

import com.dispatch.core.filter.*;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class DispatchHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(DispatchHandler.class);
    
    private final FilterChain filterChain;
    private final ExecutorService virtualThreadExecutor;
    
    public DispatchHandler(FilterChain filterChain, ExecutorService virtualThreadExecutor) {
        this.filterChain = filterChain;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest nettyRequest) {
        CompletableFuture.runAsync(() -> {
            try {
                processRequest(ctx, nettyRequest);
            } catch (Exception e) {
                logger.error("Error processing request", e);
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
            }
        }, virtualThreadExecutor);
    }
    
    private void processRequest(ChannelHandlerContext ctx, FullHttpRequest nettyRequest) {
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        
        byte[] body = new byte[nettyRequest.content().readableBytes()];
        nettyRequest.content().readBytes(body);
        
        HttpRequest request = new HttpRequest(
            nettyRequest.method(),
            nettyRequest.uri(),
            nettyRequest.headers(),
            body,
            remoteAddress
        );
        
        FilterContext context = new FilterContext(request);
        
        filterChain.execute(request, context)
            .thenAccept(result -> {
                if (result instanceof FilterResult.Respond respond) {
                    sendResponse(ctx, respond.response());
                } else {
                    sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "No response generated");
                }
            })
            .exceptionally(throwable -> {
                logger.error("Filter chain execution failed", throwable);
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
                return null;
            });
    }
    
    private void sendResponse(ChannelHandlerContext ctx, HttpResponse response) {
        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.valueOf(response.statusCode()),
            Unpooled.wrappedBuffer(response.body())
        );
        
        nettyResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        nettyResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.body().length);
        
        response.headers().forEach(entry -> {
            nettyResponse.headers().set(entry.getKey(), entry.getValue());
        });
        
        boolean keepAlive = HttpUtil.isKeepAlive(ctx.channel().attr(null).get());
        if (keepAlive) {
            nettyResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.writeAndFlush(nettyResponse);
        } else {
            ctx.writeAndFlush(nettyResponse).addListener(ChannelFutureListener.CLOSE);
        }
    }
    
    private void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            status,
            Unpooled.copiedBuffer(message.getBytes())
        );
        
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, message.length());
        
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Exception in channel handler", cause);
        sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
    }
}