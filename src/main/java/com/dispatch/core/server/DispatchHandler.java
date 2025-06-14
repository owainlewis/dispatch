package com.dispatch.core.server;

import com.dispatch.core.filter.FilterContext;
import com.dispatch.core.filter.FilterResult;
import com.dispatch.core.route.RouteManager;
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
    
    private final RouteManager routeManager;
    private final ExecutorService virtualThreadExecutor;
    
    public DispatchHandler(RouteManager routeManager, ExecutorService virtualThreadExecutor) {
        this.routeManager = routeManager;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest nettyRequest) {
        // Copy the buffer content before async processing to avoid reference counting issues
        byte[] body = new byte[nettyRequest.content().readableBytes()];
        nettyRequest.content().readBytes(body);
        
        // Create a copy of the request data we need
        HttpMethod method = nettyRequest.method();
        String uri = nettyRequest.uri();
        HttpHeaders headers = nettyRequest.headers().copy();
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        
        CompletableFuture.runAsync(() -> {
            try {
                processRequest(ctx, method, uri, headers, body, remoteAddress, nettyRequest);
            } catch (Exception e) {
                logger.error("Error processing request", e);
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
            }
        }, virtualThreadExecutor);
    }
    
    private void processRequest(ChannelHandlerContext ctx, HttpMethod method, String uri, HttpHeaders headers, 
                               byte[] body, InetSocketAddress remoteAddress, FullHttpRequest originalRequest) {
        com.dispatch.core.filter.HttpRequest request = new com.dispatch.core.filter.HttpRequest(
            method,
            uri,
            headers,
            body,
            remoteAddress
        );
        
        FilterContext context = new FilterContext(request);
        
        routeManager.processRequest(request, context)
            .thenAccept(result -> {
                if (result instanceof FilterResult.Respond respond) {
                    sendResponse(ctx, respond.response(), originalRequest);
                } else {
                    // If filters ran but none generated a response, it means no route was found
                    sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, "Not Found");
                }
            })
            .exceptionally(throwable -> {
                logger.error("Route processing failed", throwable);
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
                return null;
            });
    }
    
    private void sendResponse(ChannelHandlerContext ctx, com.dispatch.core.filter.HttpResponse response, FullHttpRequest originalRequest) {
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
        
        boolean keepAlive = HttpUtil.isKeepAlive(originalRequest);
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