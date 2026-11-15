package io.terrakube.client.model.organization.workspace;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkspaceAttributes {
    private String branch;
    private String source;
    private String name;
    private String description;
    private String folder;
    private String lastJobStatus;
    private String lastJobDate;
    private boolean locked;
    private boolean deleted;
    private boolean allowRemoteApply;
    private boolean globalRemoteState;
    // Set by Terrakube 2.34.0 and later. Older versions do not send it, which reads as false, their behaviour.
    private boolean restrictApplyToConfiguredBranch;
    private String sharedIds;
    private String defaultTemplate;
    private String lockDescription;
    private String iacType;
    private String moduleSshKey;
    private String terraformVersion;
    private String executionMode;
    private String createdBy;
    private String createdDate;
    private String updatedBy;
    private String updatedDate;
}
