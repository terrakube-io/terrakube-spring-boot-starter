package io.terrakube.client.model.organization.job;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Log {
    private Integer jobId;
    private String stepId;
    private Integer lineNumber;
    private String output;
}