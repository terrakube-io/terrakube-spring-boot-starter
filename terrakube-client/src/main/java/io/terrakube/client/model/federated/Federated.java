package io.terrakube.client.model.federated;

import io.terrakube.client.model.generic.Resource;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Federated extends Resource {
    private FederatedAttributes attributes;
    private Relationships relationships;
}
