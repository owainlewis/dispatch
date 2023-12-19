package com.dispatch.server.handlers;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;

/**
 * A [[StaticResponseHandler]] enables you to return a fixed response and may be used for e.g API Mocking
 * or returning fixed content in response to a request
 */
public final class StaticResponseHandler extends BaseHandler {

    private final int statusCode;
    private final String body;

    public StaticResponseHandler(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        logger.info("Request {}", request);
        response.setStatus(this.statusCode);
        response.write(true, BufferUtil.toBuffer(this.body), callback);

        return true;
    }
}
