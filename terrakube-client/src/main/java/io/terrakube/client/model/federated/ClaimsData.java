package io.terrakube.client.model.federated;

import io.terrakube.client.model.generic.Resource;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ClaimsData {
    private List<Resource> data;
}
