package io.terrakube.client.model.graphql.queries.search.organization;

import lombok.Data;

@Data
public class SearchOrganizationResponse {
    private OrganizationConnection organization;
}

