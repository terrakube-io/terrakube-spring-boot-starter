package io.terrakube.client.model.graphql.queries.search.organization;

import lombok.Data;

import java.util.List;

@Data
public class OrganizationConnection {
    private List<OrganizationEdge> edges;
}
