package com.warehouse.flow.engine.configuration;

import com.warehouse.flow.engine.topology.Edge;
import com.warehouse.flow.engine.topology.Node;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "warehouse")
@Getter
@Setter
public class WarehouseGraphConfig {

    private List<Node> nodes;
    private List<Edge> edges;

}
