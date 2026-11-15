package io.terrakube.client.model.organization;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Relationships {
    JobData job;
    ModuleData module;
    WorkspaceData workspace;
    ProviderData provider;
    VcsData vcs;
    TeamData team;
}
