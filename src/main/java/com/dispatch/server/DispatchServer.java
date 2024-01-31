package com.dispatch.server;

import com.dispatch.backend.StaticResponseBackend;
import com.dispatch.model.Route;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.PathMappingsHandler;
import org.eclipse.jetty.util.VirtualThreads;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

@Slf4j
public final class DispatchServer {

    private HttpClient client;

    public void start(int port) throws Exception {
        // Create an HTTP client
        HttpClient httpClient = new HttpClient();
        httpClient.setFollowRedirects(false);
        httpClient.start();

        // Create the basic server with Virtual Threads
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setVirtualThreadsExecutor(VirtualThreads.getDefaultVirtualThreadsExecutor());
        Server server = new Server(threadPool);

        // Configure connectors for the server
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        // Configure routes
        Handler.Sequence root = new Handler.Sequence();
        server.setHandler(root);
        PathMappingsHandler pathMappingsHandler = new PathMappingsHandler();
        root.addHandler(pathMappingsHandler);

        // Routes (from config)
        Route firstRoute = new Route("GET", "/v1/foo", new StaticResponseBackend(HttpStatus.OK_200, "Hello, API Gateway"));
        firstRoute.apply(pathMappingsHandler);

        server.start();

        log.info("Server started on %s".formatted(port));
    }
}