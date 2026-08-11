package io.terrakube.client.model.organization.module.version;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModuleVersionAttributes {
    private String version;
    private String commit;
    private String gitTag;
}
