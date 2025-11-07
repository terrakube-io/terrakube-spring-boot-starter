package io.terrakube.client.model.graphql.queries.search.module;

import lombok.Data;

@Data
public class VersionNode {
    private String id;
    private String version;
    private String commit;
}
