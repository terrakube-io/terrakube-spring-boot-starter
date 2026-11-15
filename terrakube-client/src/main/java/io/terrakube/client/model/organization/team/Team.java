package io.terrakube.client.model.organization.team;

import io.terrakube.client.model.generic.Resource;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Team extends Resource {
    private TeamAttributes attributes;
    private Relationships relationships;
}
