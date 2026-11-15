package io.terrakube.client.security;

/**
 * Classification of an incoming JWT caller against Terrakube's trust boundaries.
 */
public enum CallerType {
    /**
     * Per-job runner token minted for an executing step ({@code iss=TerrakubeInternal},
     * {@code sub=TerrakubeInternal (EXECUTOR)}, with {@code organizationId}, {@code workspaceId},
     * {@code jobId}, and {@code stepId}).
     */
    WORKSPACE_EXECUTOR,

    /**
     * Registry service-to-service token ({@code iss=TerrakubeInternal},
     * {@code sub=TerrakubeInternal (REGISTRY)}, without workspace or organization scoping).
     */
    REGISTRY_SERVICE,

    /**
     * Executor daemon token ({@code iss=TerrakubeInternal},
     * {@code sub=TerrakubeInternal (EXECUTOR)}, without workspace or organization scoping).
     */
    EXECUTOR_SERVICE,

    /**
     * Internal or unrecognized token with incomplete, legacy, or untrusted claims (fail-closed).
     */
    UNKNOWN_INTERNAL,

    /**
     * External OIDC workload identity validated against {@code Federated} and {@code FederatedClaim}
     * predicates (for example GitHub Actions OIDC).
     */
    FEDERATED,

    /**
     * Terrakube Personal Access Token or Team Token ({@code iss=Terrakube}).
     */
    PAT_OR_TEAM_TOKEN,

    /**
     * Interactive user authenticated via the configured Dex / OIDC issuer.
     */
    OIDC_USER
}
