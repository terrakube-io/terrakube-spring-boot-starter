package io.terrakube.client.model.organization.workspace.access;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccessAttributes {
    private String name;
    private String role;
    private boolean manageState;
    private boolean manageJob;
    private boolean manageWorkspace;
    private boolean planJob;
    private boolean approveJob;
    private boolean destroyJob = true;
}
