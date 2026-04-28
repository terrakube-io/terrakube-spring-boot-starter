package io.terrakube.client.model.organization.job;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobAttributes {
    private String output;
    private String status;
    private String overrideSource;
    private String terraformPlan;
    private String tcl;
    private boolean planChanges;
    private String approvalTeam;
    private String templateReference;
    private String commitId;
}
