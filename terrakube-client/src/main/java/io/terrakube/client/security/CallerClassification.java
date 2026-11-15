package io.terrakube.client.security;

import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.Set;

/**
 * Immutable classification result produced by {@link TerrakubeCallerClassifier}.
 */
@Value
@Builder
public class CallerClassification {
    CallerType callerType;
    String issuer;
    String subject;
    String email;
    String organizationId;
    String workspaceId;
    String jobId;
    String stepId;
    @Builder.Default
    Set<String> effectiveGroups = Collections.emptySet();
    boolean federatedClaimsVerified;
}
