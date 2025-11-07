package io.terrakube.client.model.graphql.queries.search.module;

import lombok.Data;

@Data
public class ModuleNode {
    private String id;
    private String name;
    private String provider;
    private VersionConnection version;
}
