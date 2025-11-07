package io.terrakube.client.model.graphql.queries.search.module;

import lombok.Data;
import java.util.List;

@Data
public class ModuleConnection {
    private List<ModuleEdge> edges;
}
