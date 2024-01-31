package com.dispatch;

import com.dispatch.config.Config;
import com.dispatch.config.ConfigReader;
import com.dispatch.server.DispatchServer;

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