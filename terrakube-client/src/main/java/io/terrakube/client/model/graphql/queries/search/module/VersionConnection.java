package io.terrakube.client.model.graphql.queries.search.module;

import lombok.Data;
import java.util.List;

@Data
public class VersionConnection {
    private List<VersionEdge> edges;
}
