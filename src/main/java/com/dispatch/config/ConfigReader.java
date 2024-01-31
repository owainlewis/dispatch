package com.dispatch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;

public final class ConfigReader {

    private final ObjectMapper mapper;

    public ConfigReader() {
        this.mapper = new ObjectMapper(new YAMLFactory());
    }

    public Config read(String pathName) throws IOException {
        return mapper.readValue(new File(pathName), Config.class);
    }
}
