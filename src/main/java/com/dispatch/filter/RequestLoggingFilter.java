package com.dispatch.filter;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

@Slf4j
public class RequestLoggingFilter extends Handler.Wrapper {

    public RequestLoggingFilter(Handler handler) {
        super(handler);
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception
    {
        log.info("Handling request {}", request);

        return super.handle(request, response, callback);
    }
}