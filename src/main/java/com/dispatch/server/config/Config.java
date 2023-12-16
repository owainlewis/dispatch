package com.dispatch.server.config;

import java.util.List;

public class Config {

    private List<Route> routes;

    public Config() {

    }

    public List<Route> getRoutes() {
        return routes;
    }

    public void setRoutes(List<Route> routes) {
        this.routes = routes;
    }

}
