package com.dispatch.model;

import com.dispatch.backend.Backend;
import org.eclipse.jetty.http.pathmap.PathSpec;
import org.eclipse.jetty.server.handler.PathMappingsHandler;

public class Route {

    private String method;

    private final String route;

    private final Backend backend;

    public Route(String method, String route, Backend backend) {
        this.method = method;
        this.route = route;
        this.backend = backend;
    }

    public void apply(PathMappingsHandler mapping) {
        mapping.addMapping(PathSpec.from(route), backend);
    }
}
