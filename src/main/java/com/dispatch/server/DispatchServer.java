package com.dispatch.server;

import com.dispatch.server.handlers.ProxyHandler;
import com.dispatch.server.handlers.StaticResponseHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.VirtualThreads;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DispatchServer {

    private static final Logger logger = LoggerFactory.getLogger(DispatchServer.class);

    private QueuedThreadPool buildThreadPool() {
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setVirtualThreadsExecutor(VirtualThreads.getDefaultVirtualThreadsExecutor());
        return threadPool;
    }

    /**
     * Builds the handlers from configuration to handle incoming requests
     *
     * @return
     * @throws Exception
     */
    private ContextHandlerCollection buildHandlerCollection() throws Exception {
        ContextHandlerCollection collection = new ContextHandlerCollection();
        // Static Response
        StaticResponseHandler staticHandler = new StaticResponseHandler(200, "Hello, World");
        // Proxy Response
        ProxyHandler proxyHandler = new ProxyHandler();

        // Filter Chain
        collection.addHandler(staticHandler);

        return collection;
    }

    public void start(int port) throws Exception {

        Server server = new Server(this.buildThreadPool());

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        ContextHandlerCollection collection = this.buildHandlerCollection();

        // Start the server and configure it
        server.setHandler(collection);
        server.start();

        logger.info("Server started on %s".formatted(port));
    }
}