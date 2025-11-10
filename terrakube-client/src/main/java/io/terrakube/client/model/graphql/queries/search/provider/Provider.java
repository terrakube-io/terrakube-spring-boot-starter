package io.terrakube.client.model.graphql.queries.search.provider;

import lombok.Data;

@Data
public class Provider {
    private String id;
    private String name;
    private VersionConnection version;
}
