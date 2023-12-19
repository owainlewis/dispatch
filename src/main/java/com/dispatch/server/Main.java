package com.dispatch.server;

import com.dispatch.server.config.Config;
import com.dispatch.server.config.ConfigReader;

public class Main {
    private static final String CONFIG = "src/main/resources/config.yml";

    public static void main(String[] args) throws Exception {
        // Load configuration
        ConfigReader reader = new ConfigReader();
        Config config = reader.read(CONFIG);

        // Start server with defaults
        new DispatchServer().start(8080);
    }
}
