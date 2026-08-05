package io.terrakube.client.model.federated;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FederatedAttributes {
    private String name;
    private String issuerUrl;
    private String audience;
}
