package io.terrakube.client.model.graphql.queries.search.provider;

import lombok.Data;

@Data
public class SearchOrganizationProviderResponse {
    private OrganizationConnection organization;
}
