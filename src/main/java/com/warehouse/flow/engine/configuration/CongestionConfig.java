package com.warehouse.flow.engine.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "warehouse.congestion")
@Getter
@Setter
public class CongestionConfig {

    private double mediumThreshold;
    private double highThreshold;
}