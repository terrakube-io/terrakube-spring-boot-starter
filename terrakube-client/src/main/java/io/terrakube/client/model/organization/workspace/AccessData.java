package io.terrakube.client.model.organization.workspace;

import io.terrakube.client.model.generic.Resource;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AccessData {
    private List<Resource> data;
}
