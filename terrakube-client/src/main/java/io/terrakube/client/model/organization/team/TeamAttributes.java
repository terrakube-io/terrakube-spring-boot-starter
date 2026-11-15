package io.terrakube.client.model.organization.team;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeamAttributes {
    private String name;
    private String role;
    private boolean manageState;
    private boolean manageCollection;
    private boolean manageJob;
    private boolean manageWorkspace;
    private boolean manageModule;
    private boolean manageProvider;
    private boolean manageVcs;
    private boolean manageTemplate;
    private boolean managePolicies;
    private boolean planJob;
    private boolean approveJob;
    private boolean destroyJob = true;
}
