package io.terrakube.client.security;

import io.terrakube.client.TerrakubeClient;
import io.terrakube.client.model.federated.Federated;
import io.terrakube.client.model.federated.claim.FederatedClaim;
import io.terrakube.client.model.generic.Resource;
import io.terrakube.client.model.organization.job.step.Step;
import io.terrakube.client.model.organization.team.Team;
import io.terrakube.client.model.organization.workspace.Workspace;
import io.terrakube.client.model.organization.workspace.access.Access;
import io.terrakube.client.model.response.Response;
import io.terrakube.client.model.response.ResponseWithInclude;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Classifies incoming JWT callers and evaluates fail-closed organization authorization.
 */
@Slf4j
public final class TerrakubeCallerClassifier {

    public static final String INTERNAL_ISSUER = "TerrakubeInternal";
    public static final String PAT_ISSUER = "Terrakube";
    public static final String SUBJECT_EXECUTOR = "TerrakubeInternal (EXECUTOR)";
    public static final String SUBJECT_REGISTRY = "TerrakubeInternal (REGISTRY)";

    private static final Set<String> ACTIVE_STEP_STATUSES = Set.of(
            "running",
            "queue",
            "pending",
            "waitingApproval",
            "approved"
    );

    private TerrakubeCallerClassifier() {
    }

    /**
     * Classifies a caller from decoded JWT claims.
     *
     * @param tokenClaims decoded JWT claim map
     * @param oidcIssuerUri configured Dex/OIDC issuer URI for interactive users
     * @param client Terrakube API client used to verify federated OIDC credentials
     * @return immutable caller classification (fails closed to {@link CallerType#UNKNOWN_INTERNAL} on missing claims)
     */
    public static CallerClassification classify(
            Map<String, Object> tokenClaims,
            String oidcIssuerUri,
            TerrakubeClient client) {
        if (tokenClaims == null || tokenClaims.isEmpty()) {
            return unknownInternal(null, null);
        }

        String issuer = extractString(tokenClaims, "iss");
        String subject = extractString(tokenClaims, "sub");
        if (isBlank(issuer)) {
            return unknownInternal(issuer, subject);
        }

        if (INTERNAL_ISSUER.equals(issuer)) {
            return classifyInternal(tokenClaims, issuer, subject);
        }

        if (PAT_ISSUER.equals(issuer)) {
            return CallerClassification.builder()
                    .callerType(CallerType.PAT_OR_TEAM_TOKEN)
                    .issuer(issuer)
                    .subject(subject)
                    .email(normalizeEmail(tokenClaims.get("email")))
                    .effectiveGroups(normalizeGroups(tokenClaims.get("groups")))
                    .federatedClaimsVerified(false)
                    .build();
        }

        if (!isBlank(oidcIssuerUri) && oidcIssuerUri.equals(issuer)) {
            return CallerClassification.builder()
                    .callerType(CallerType.OIDC_USER)
                    .issuer(issuer)
                    .subject(subject)
                    .email(normalizeEmail(tokenClaims.get("email")))
                    .effectiveGroups(normalizeGroups(tokenClaims.get("groups")))
                    .federatedClaimsVerified(false)
                    .build();
        }

        return classifyFederated(tokenClaims, issuer, subject, client);
    }

    /**
     * Evaluates whether a classified caller is authorized for {@code targetOrganizationId}.
     *
     * @param caller classification produced by {@link #classify(Map, String, TerrakubeClient)}
     * @param targetOrganizationId organization UUID being accessed
     * @param instanceOwner configured instance owner identifier
     * @param client Terrakube API client used for step, team, and workspace access checks
     * @return {@code true} only when all caller-type invariants and API checks succeed
     */
    public static boolean isAuthorizedForOrganization(
            CallerClassification caller,
            String targetOrganizationId,
            String instanceOwner,
            TerrakubeClient client) {
        if (caller == null || caller.getCallerType() == null) {
            return false;
        }

        switch (caller.getCallerType()) {
            case REGISTRY_SERVICE:
                return true;
            case EXECUTOR_SERVICE:
            case UNKNOWN_INTERNAL:
                return false;
            case WORKSPACE_EXECUTOR:
                return isAuthorizedWorkspaceExecutor(caller, targetOrganizationId, client);
            case FEDERATED:
                return isAuthorizedFederated(caller, targetOrganizationId, instanceOwner, client);
            case OIDC_USER:
            case PAT_OR_TEAM_TOKEN:
                return isAuthorizedUserOrPat(caller, targetOrganizationId, instanceOwner, client);
            default:
                return false;
        }
    }

    private static CallerClassification classifyInternal(
            Map<String, Object> tokenClaims,
            String issuer,
            String subject) {
        String organizationId = extractString(tokenClaims, "organizationId");
        String workspaceId = extractString(tokenClaims, "workspaceId");
        String jobId = extractStringOrNumber(tokenClaims, "jobId");
        String stepId = extractString(tokenClaims, "stepId");
        String jti = extractString(tokenClaims, "jti");

        String[] resolvedIds = resolveJobAndStepFromJti(jobId, stepId, jti);
        jobId = resolvedIds[0];
        stepId = resolvedIds[1];

        boolean hasScopeKey = tokenClaims.containsKey("workspaceId") || tokenClaims.containsKey("organizationId");
        if (hasScopeKey) {
            boolean validRunner = SUBJECT_EXECUTOR.equals(subject)
                    && !isBlank(workspaceId)
                    && !isBlank(organizationId)
                    && !isBlank(jobId)
                    && !isBlank(stepId);
            return CallerClassification.builder()
                    .callerType(validRunner ? CallerType.WORKSPACE_EXECUTOR : CallerType.UNKNOWN_INTERNAL)
                    .issuer(issuer)
                    .subject(subject)
                    .email("")
                    .organizationId(organizationId)
                    .workspaceId(workspaceId)
                    .jobId(jobId)
                    .stepId(stepId)
                    .effectiveGroups(Collections.emptySet())
                    .federatedClaimsVerified(false)
                    .build();
        }

        CallerType serviceType;
        if (SUBJECT_REGISTRY.equals(subject)) {
            serviceType = CallerType.REGISTRY_SERVICE;
        } else if (SUBJECT_EXECUTOR.equals(subject)) {
            serviceType = CallerType.EXECUTOR_SERVICE;
        } else {
            serviceType = CallerType.UNKNOWN_INTERNAL;
        }

        return CallerClassification.builder()
                .callerType(serviceType)
                .issuer(issuer)
                .subject(subject)
                .email("")
                .effectiveGroups(Collections.emptySet())
                .federatedClaimsVerified(false)
                .build();
    }

    private static CallerClassification classifyFederated(
            Map<String, Object> tokenClaims,
            String issuer,
            String subject,
            TerrakubeClient client) {
        List<String> audiences = extractAudiences(tokenClaims.get("aud"));
        if (audiences.isEmpty() || client == null) {
            return unverifiedFederated(issuer, subject);
        }

        Set<String> matchedNames = new LinkedHashSet<>();
        try {
            for (String audience : audiences) {
                ResponseWithInclude<List<Federated>, FederatedClaim> response =
                        client.getFederatedByIssuerUrlAndAudienceWithClaims(issuer, audience);
                collectMatchedFederatedNames(response, tokenClaims, matchedNames);
            }
        } catch (Exception ex) {
            log.warn("Federated credential verification failed for issuer {}: {}", issuer, ex.getMessage());
            return unverifiedFederated(issuer, subject);
        }

        Set<String> effectiveGroups = Collections.unmodifiableSet(matchedNames);
        return CallerClassification.builder()
                .callerType(CallerType.FEDERATED)
                .issuer(issuer)
                .subject(subject)
                .email("")
                .effectiveGroups(effectiveGroups)
                .federatedClaimsVerified(!effectiveGroups.isEmpty())
                .build();
    }

    private static void collectMatchedFederatedNames(
            ResponseWithInclude<List<Federated>, FederatedClaim> response,
            Map<String, Object> tokenClaims,
            Set<String> matchedNames) {
        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            return;
        }

        Map<String, FederatedClaim> includedClaims = new HashMap<>();
        if (response.getIncluded() != null) {
            for (FederatedClaim claim : response.getIncluded()) {
                if (claim != null && !isBlank(claim.getId())) {
                    includedClaims.put(claim.getId(), claim);
                }
            }
        }

        for (Federated federated : response.getData()) {
            if (federated != null && matchesAllFederatedClaims(federated, includedClaims, tokenClaims)) {
                String name = federated.getAttributes() != null ? federated.getAttributes().getName() : null;
                if (!isBlank(name)) {
                    matchedNames.add(name.trim());
                }
            }
        }
    }

    private static boolean matchesAllFederatedClaims(
            Federated federated,
            Map<String, FederatedClaim> includedClaims,
            Map<String, Object> tokenClaims) {
        if (federated.getRelationships() == null
                || federated.getRelationships().getClaims() == null
                || federated.getRelationships().getClaims().getData() == null) {
            return false;
        }

        List<Resource> claimRefs = federated.getRelationships().getClaims().getData();
        if (claimRefs.isEmpty()) {
            return false;
        }

        for (Resource claimRef : claimRefs) {
            if (claimRef == null || isBlank(claimRef.getId())) {
                return false;
            }
            FederatedClaim claim = includedClaims.get(claimRef.getId());
            if (claim == null || claim.getAttributes() == null) {
                return false;
            }
            String claimKey = claim.getAttributes().getClaimKey();
            String claimValue = claim.getAttributes().getClaimValue();
            if (isBlank(claimKey) || claimValue == null) {
                return false;
            }
            Object tokenValue = tokenClaims.get(claimKey);
            if (!claimValueMatches(tokenValue, claimValue)) {
                return false;
            }
        }
        return true;
    }

    private static boolean claimValueMatches(Object tokenValue, String expected) {
        if (tokenValue == null) {
            return false;
        }
        if (tokenValue instanceof Collection<?>) {
            for (Object item : (Collection<?>) tokenValue) {
                if (item != null && expected.equals(item.toString())) {
                    return true;
                }
            }
            return false;
        }
        if (tokenValue instanceof Object[]) {
            for (Object item : (Object[]) tokenValue) {
                if (item != null && expected.equals(item.toString())) {
                    return true;
                }
            }
            return false;
        }
        return expected.equals(tokenValue.toString());
    }

    private static boolean isAuthorizedWorkspaceExecutor(
            CallerClassification caller,
            String targetOrganizationId,
            TerrakubeClient client) {
        if (client == null
                || isBlank(targetOrganizationId)
                || isBlank(caller.getOrganizationId())
                || !caller.getOrganizationId().equalsIgnoreCase(targetOrganizationId)
                || isBlank(caller.getJobId())
                || !isValidUuid(caller.getStepId())) {
            return false;
        }

        try {
            Response<Step> stepResponse = client.getStepById(
                    targetOrganizationId,
                    caller.getJobId(),
                    caller.getStepId()
            );
            if (stepResponse == null
                    || stepResponse.getData() == null
                    || stepResponse.getData().getAttributes() == null) {
                return false;
            }
            String status = stepResponse.getData().getAttributes().getStatus();
            return !isBlank(status) && ACTIVE_STEP_STATUSES.contains(status);
        } catch (Exception ex) {
            log.warn("Failed to verify runner step {} for job {} in org {}: {}",
                    caller.getStepId(), caller.getJobId(), targetOrganizationId, ex.getMessage());
            return false;
        }
    }

    private static boolean isAuthorizedFederated(
            CallerClassification caller,
            String targetOrganizationId,
            String instanceOwner,
            TerrakubeClient client) {
        if (!caller.isFederatedClaimsVerified()
                || caller.getEffectiveGroups() == null
                || caller.getEffectiveGroups().isEmpty()
                || isBlank(targetOrganizationId)) {
            return false;
        }

        if (!isBlank(instanceOwner) && caller.getEffectiveGroups().contains(instanceOwner)) {
            return true;
        }

        return hasTeamOrAccessMembership(caller.getEffectiveGroups(), targetOrganizationId, client);
    }

    private static boolean isAuthorizedUserOrPat(
            CallerClassification caller,
            String targetOrganizationId,
            String instanceOwner,
            TerrakubeClient client) {
        if (isBlank(targetOrganizationId)) {
            return false;
        }

        Set<String> groups = caller.getEffectiveGroups() != null
                ? caller.getEffectiveGroups()
                : Collections.emptySet();

        if (!isBlank(instanceOwner)) {
            if (instanceOwner.equals(caller.getEmail()) || groups.contains(instanceOwner)) {
                return true;
            }
        }

        if (groups.isEmpty()) {
            return false;
        }

        return hasTeamOrAccessMembership(groups, targetOrganizationId, client);
    }

    private static boolean hasTeamOrAccessMembership(
            Set<String> groups,
            String targetOrganizationId,
            TerrakubeClient client) {
        if (client == null || groups.isEmpty()) {
            return false;
        }

        try {
            Response<List<Team>> teamsResponse = client.getAllTeams(targetOrganizationId);
            if (teamsResponse != null && teamsResponse.getData() != null) {
                for (Team team : teamsResponse.getData()) {
                    if (team != null && team.getAttributes() != null) {
                        String teamName = team.getAttributes().getName();
                        if (!isBlank(teamName) && groups.contains(teamName)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to fetch teams for org {}: {}", targetOrganizationId, ex.getMessage());
        }

        try {
            ResponseWithInclude<List<Workspace>, Access> workspacesResponse =
                    client.getWorkspacesWithAccess(targetOrganizationId);
            if (workspacesResponse != null && workspacesResponse.getIncluded() != null) {
                for (Access access : workspacesResponse.getIncluded()) {
                    if (access != null && access.getAttributes() != null) {
                        String accessName = access.getAttributes().getName();
                        if (!isBlank(accessName) && groups.contains(accessName)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to fetch workspace access for org {}: {}", targetOrganizationId, ex.getMessage());
        }

        return false;
    }

    private static CallerClassification unknownInternal(String issuer, String subject) {
        return CallerClassification.builder()
                .callerType(CallerType.UNKNOWN_INTERNAL)
                .issuer(issuer)
                .subject(subject)
                .email("")
                .effectiveGroups(Collections.emptySet())
                .federatedClaimsVerified(false)
                .build();
    }

    private static CallerClassification unverifiedFederated(String issuer, String subject) {
        return CallerClassification.builder()
                .callerType(CallerType.FEDERATED)
                .issuer(issuer)
                .subject(subject)
                .email("")
                .effectiveGroups(Collections.emptySet())
                .federatedClaimsVerified(false)
                .build();
    }

    private static String[] resolveJobAndStepFromJti(String jobId, String stepId, String jti) {
        String resolvedJobId = jobId;
        String resolvedStepId = stepId;
        if (isBlank(jti)) {
            return new String[]{resolvedJobId, resolvedStepId};
        }

        if (!isBlank(resolvedJobId) && isBlank(resolvedStepId) && jti.startsWith(resolvedJobId + "-")) {
            String suffix = jti.substring(resolvedJobId.length() + 1).trim();
            if (!suffix.isEmpty()) {
                resolvedStepId = suffix;
            }
            return new String[]{resolvedJobId, resolvedStepId};
        }

        if (isBlank(resolvedJobId) && !isBlank(resolvedStepId) && jti.endsWith("-" + resolvedStepId)) {
            String prefix = jti.substring(0, jti.length() - resolvedStepId.length() - 1).trim();
            if (!prefix.isEmpty()) {
                resolvedJobId = prefix;
            }
            return new String[]{resolvedJobId, resolvedStepId};
        }

        if (isBlank(resolvedJobId) && isBlank(resolvedStepId)) {
            // Runner tokens format jti as "<jobId>-<stepUuid>" where stepUuid is 36 chars.
            if (jti.length() > 37 && jti.charAt(jti.length() - 37) == '-') {
                String candidateUuid = jti.substring(jti.length() - 36);
                String candidateJobId = jti.substring(0, jti.length() - 37).trim();
                if (!candidateJobId.isEmpty() && isValidUuid(candidateUuid)) {
                    return new String[]{candidateJobId, candidateUuid};
                }
            }
            int dashIdx = jti.indexOf('-');
            if (dashIdx > 0 && dashIdx < jti.length() - 1) {
                String prefix = jti.substring(0, dashIdx).trim();
                String suffix = jti.substring(dashIdx + 1).trim();
                if (!prefix.isEmpty() && !suffix.isEmpty()) {
                    return new String[]{prefix, suffix};
                }
            } else {
                resolvedJobId = jti;
            }
        }
        return new String[]{resolvedJobId, resolvedStepId};
    }

    private static String extractString(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        if (value instanceof String) {
            String trimmed = ((String) value).trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
        return null;
    }

    private static String extractStringOrNumber(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        if (value instanceof String) {
            String trimmed = ((String) value).trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
        if (value instanceof Number) {
            return String.valueOf(value);
        }
        return null;
    }

    private static String normalizeEmail(Object rawEmail) {
        if (rawEmail instanceof String) {
            return ((String) rawEmail).trim();
        }
        return "";
    }

    private static Set<String> normalizeGroups(Object rawGroups) {
        if (rawGroups == null) {
            return Collections.emptySet();
        }
        Collection<?> items;
        if (rawGroups instanceof Object[]) {
            items = Arrays.asList((Object[]) rawGroups);
        } else if (rawGroups instanceof Collection<?>) {
            items = (Collection<?>) rawGroups;
        } else if (rawGroups instanceof String) {
            String trimmed = ((String) rawGroups).trim();
            return trimmed.isEmpty() ? Collections.emptySet() : Collections.singleton(trimmed);
        } else {
            return Collections.emptySet();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (Object item : items) {
            if (item != null) {
                String group = item.toString().trim();
                if (!group.isEmpty()) {
                    normalized.add(group);
                }
            }
        }
        return normalized.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(normalized);
    }

    private static List<String> extractAudiences(Object rawAudience) {
        if (rawAudience == null) {
            return Collections.emptyList();
        }
        Set<String> audiences = new LinkedHashSet<>();
        if (rawAudience instanceof String) {
            String value = ((String) rawAudience).trim();
            if (!value.isEmpty()) {
                audiences.add(value);
            }
        } else if (rawAudience instanceof Collection<?>) {
            for (Object item : (Collection<?>) rawAudience) {
                if (item instanceof String) {
                    String value = ((String) item).trim();
                    if (!value.isEmpty()) {
                        audiences.add(value);
                    }
                }
            }
        } else if (rawAudience instanceof Object[]) {
            for (Object item : (Object[]) rawAudience) {
                if (item instanceof String) {
                    String value = ((String) item).trim();
                    if (!value.isEmpty()) {
                        audiences.add(value);
                    }
                }
            }
        }
        return audiences.isEmpty() ? Collections.emptyList() : new ArrayList<>(audiences);
    }

    private static boolean isValidUuid(String value) {
        if (isBlank(value)) {
            return false;
        }
        try {
            UUID.fromString(value.trim());
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
