package io.terrakube.client.model.organization;

import io.terrakube.client.model.generic.Resource;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TeamData {
    private List<Resource> data;
}
