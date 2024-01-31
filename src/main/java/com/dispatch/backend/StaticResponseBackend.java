package com.dispatch.backend;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;

public class StaticResponseBackend extends Backend {

    private final int statusCode;

    private final String body;

    public StaticResponseBackend(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        response.setStatus(this.statusCode);
        response.write(true, BufferUtil.toBuffer(this.body), callback);
        return true;
    }
}
