package com.dispatch.server;

import com.dispatch.server.config.Config;
import com.dispatch.server.config.ConfigReader;

public class Main {

    public static void main(String[] args) throws Exception {

        ConfigReader reader = new ConfigReader();

        Config config = reader.read("src/main/resources/config.yml");

        System.out.println(config);

        new DispatchServer().start(8080);
    }
}
