package com.dispatch.server.handlers;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.nio.ByteBuffer;

public class ProxyHandler extends BaseHandler implements AutoCloseable {

    private final HttpClient client;

    public ProxyHandler() throws Exception {
        HttpClient httpClient = new HttpClient();
        httpClient.setFollowRedirects(false);
        httpClient.start();

        this.client = httpClient;
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        logger.info("Received request {}", request);

        // Asynchronously
        this.client.newRequest("https://postman-echo.com/get").send(new org.eclipse.jetty.client.Response.Listener() {
            @Override
            public void onContent(org.eclipse.jetty.client.Response backendResponse, ByteBuffer body) {
                response.setStatus(backendResponse.getStatus());
                // Copy across all HTTP headers except for Content-Length
                backendResponse.getHeaders().stream().forEach((httpField -> {
                    if (!httpField.getName().equalsIgnoreCase("Content-Length")) {
                        response.getHeaders().put(httpField);
                    }
                }));

                response.write(true, body, callback);
            }
        });

        return true;
    }

    @Override
    public void close() throws Exception {
        this.client.stop();
    }
}
