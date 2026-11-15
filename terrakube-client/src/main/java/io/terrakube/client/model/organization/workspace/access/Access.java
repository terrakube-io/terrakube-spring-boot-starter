package io.terrakube.client.model.organization.workspace.access;

import io.terrakube.client.model.generic.Resource;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Access extends Resource {
    private AccessAttributes attributes;
    private Relationships relationships;
}
