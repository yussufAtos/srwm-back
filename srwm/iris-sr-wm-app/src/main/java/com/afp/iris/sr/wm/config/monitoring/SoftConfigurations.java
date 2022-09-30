package com.afp.iris.sr.wm.config.monitoring;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "softconfig")
public class SoftConfigurations {

    private String domain;
    private String project;
    private String projectType;
    private String owner;
    private String version;
    private String[] communication;
    private String[] applicationPorts;
    private String jmxPort;
    private Map<String, String> languages;
    private Map<String, String> internalDependencies;
    @Setter(AccessLevel.NONE)
    private Map<String, String> databases = Collections.emptyMap();
    private String packaging;

}

