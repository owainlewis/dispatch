package com.dispatch.server.handlers;

import org.eclipse.jetty.server.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseHandler extends Handler.Abstract {

    protected Logger logger = LoggerFactory.getLogger(BaseHandler.class);

}
