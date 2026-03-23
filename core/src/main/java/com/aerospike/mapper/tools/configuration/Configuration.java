package com.aerospike.mapper.tools.configuration;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Configuration {
    private final List<ClassConfig> classes;

    public Configuration() {
        this.classes = new ArrayList<>();
    }

    public void add(ClassConfig config) {
        this.classes.add(config);
    }
}
