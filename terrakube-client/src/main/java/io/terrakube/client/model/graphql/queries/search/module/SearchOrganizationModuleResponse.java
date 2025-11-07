package io.terrakube.client.model.graphql.queries.search.module;

import lombok.Data;

@Data
public class SearchOrganizationModuleResponse {
    private OrganizationConnection organization;
}
