package com.warehouse.flow.engine;

import com.warehouse.flow.engine.configuration.WarehouseGraphConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@ConfigurationPropertiesScan
//@EnableConfigurationProperties(WarehouseGraphConfig.class)
public class WarehouseFlowEngineApplication {

    public static void main(String[] args) {

        SpringApplication.run(WarehouseFlowEngineApplication.class, args);
    }

}
