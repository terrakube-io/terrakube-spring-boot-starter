package io.terrakube.client.model.organization.workspace;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Relationships {
    JobData job;
    OrganizationData organization;
    VariableData variable;
    VcsData vcs;
    SshData ssh;
    HistoryData history;
    AccessData access;
}
