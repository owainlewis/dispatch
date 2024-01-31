package com.dispatch.handler;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.nio.charset.StandardCharsets;

public class DefaultHandler extends Handler.Abstract.NonBlocking {

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {

        response.setStatus(HttpStatus.NOT_FOUND_404);
        response.write(true, StandardCharsets.UTF_8.encode("Not Found"), callback);

        return true;
    }
}
