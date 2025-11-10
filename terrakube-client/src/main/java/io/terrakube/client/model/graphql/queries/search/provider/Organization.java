package io.terrakube.client.model.graphql.queries.search.provider;

import lombok.Data;

@Data
public class Organization {
    private String id;
    private String name;
    private ProviderConnection provider;
}
