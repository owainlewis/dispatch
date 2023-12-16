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
    public boolean handle(Request request, Response jettyResponse, Callback callback) throws Exception {

        logger.info("Received request {}", request);

        // Asynchronously
        this.client.newRequest("https://postman-echo.com/get").send(new org.eclipse.jetty.client.Response.Listener() {
            @Override
            public void onContent(org.eclipse.jetty.client.Response response, ByteBuffer body) {
                jettyResponse.setStatus(response.getStatus());
                // Copy across all HTTP headers except for Content-Length
                response.getHeaders().stream().forEach((httpField -> {
                    if (!httpField.getName().equalsIgnoreCase("Content-Length")) {
                        jettyResponse.getHeaders().put(httpField);
                    }
                }));

                jettyResponse.write(true, body, callback);

            }
        });

        return true;
    }

    @Override
    public void close() throws Exception {
        this.client.stop();
    }
}
