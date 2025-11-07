package io.terrakube.client.model.graphql.queries.search.module;

import lombok.Data;

@Data
public class OrganizationNode {
    private String id;
    private String name;
    private ModuleConnection module;
}
