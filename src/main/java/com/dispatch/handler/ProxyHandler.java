package com.dispatch.handler;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.nio.ByteBuffer;

@Slf4j
public final class ProxyHandler extends Handler.Abstract {

    private final HttpClient client;

    public ProxyHandler(HttpClient client) {
        this.client = client;
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        log.info("Request: {} {}", request.getMethod(), request.getHttpURI().getPath());

        try {
            long start = System.currentTimeMillis();

            ContentResponse clientResponse = this.client.newRequest("https://postman-echo.com/get")
                    .send();

            long finish = System.currentTimeMillis();
            long timeElapsed = finish - start;

            log.info("Response took {}", clientResponse.getStatus());

            // Write response
            response.setStatus(clientResponse.getStatus());
            response.write(true,ByteBuffer.wrap(clientResponse.getContent()), null);
        } catch (Exception e){
            log.info("Exception while handling request: {}", e.getMessage());
            response.setStatus(500);
            response.write(true, ByteBuffer.wrap("Internal Server Error".getBytes()), null);
        }

        callback.succeeded();
        return true;
    }
}
