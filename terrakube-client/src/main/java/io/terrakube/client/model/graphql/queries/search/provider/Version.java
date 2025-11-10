package io.terrakube.client.model.graphql.queries.search.provider;

import lombok.Data;
import java.util.List;

@Data
public class Version {
    private String id;
    private String versionNumber;
    private String protocols;
    private ImplementationConnection implementation;
}
