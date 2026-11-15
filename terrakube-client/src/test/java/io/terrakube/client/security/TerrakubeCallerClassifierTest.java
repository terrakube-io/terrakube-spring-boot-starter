package io.terrakube.client.security;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import feign.Feign;
import feign.http2client.Http2Client;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import io.terrakube.client.TerrakubeClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@WireMockTest
class TerrakubeCallerClassifierTest {

    private static final String OIDC_ISSUER = "https://dex.terrakube.example/dex";
    private static final String GITHUB_ISSUER = "https://token.actions.githubusercontent.com";
    private static final String ORG_ID = "11111111-1111-1111-1111-111111111111";
    private static final String OTHER_ORG_ID = "22222222-2222-2222-2222-222222222222";
    private static final String WS_ID = "33333333-3333-3333-3333-333333333333";
    private static final String STEP_UUID = "44444444-4444-4444-4444-444444444444";

    private TerrakubeClient client;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        client = Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .client(new Http2Client())
                .target(TerrakubeClient.class, wmRuntimeInfo.getHttpBaseUrl());
    }

    // ──────────────────────────────── Internal Tokens ────────────────────────────────

    @Test
    void classify_nullOrEmptyClaims_failsClosedAsUnknownInternal() {
        CallerClassification nullResult = TerrakubeCallerClassifier.classify(null, OIDC_ISSUER, client);
        assertEquals(CallerType.UNKNOWN_INTERNAL, nullResult.getCallerType());
        assertTrue(nullResult.getEffectiveGroups().isEmpty());
        assertFalse(TerrakubeCallerClassifier.isAuthorizedForOrganization(nullResult, ORG_ID, "owner@example.com", client));

        CallerClassification emptyResult = TerrakubeCallerClassifier.classify(Map.of(), OIDC_ISSUER, client);
        assertEquals(CallerType.UNKNOWN_INTERNAL, emptyResult.getCallerType());
        assertFalse(TerrakubeCallerClassifier.isAuthorizedForOrganization(emptyResult, ORG_ID, "owner@example.com", client));
    }

    @Test
    void classify_registryServiceToken_authorizesOrganization() {
        Map<String, Object> claims = Map.of(
                "iss", "TerrakubeInternal",
                "sub", "TerrakubeInternal (REGISTRY)",
                "email", "no-reply@terrakube.io"
        );

        CallerClassification caller = TerrakubeCallerClassifier.classify(claims, OIDC_ISSUER, client);

        assertEquals(CallerType.REGISTRY_SERVICE, caller.getCallerType());
        assertTrue(caller.getEffectiveGroups().isEmpty());
        assertTrue(TerrakubeCallerClassifier.isAuthorizedForOrganization(caller, ORG_ID, "", client));
    }

    @Test
    void classify_executorServiceAndUnknownInternalTokens_failClosed() {
        Map<String, Object> executorServiceClaims = Map.of(
                "iss", "TerrakubeInternal",
                "sub", "TerrakubeInternal (EXECUTOR)"
        );
        CallerClassification executorService = TerrakubeCallerClassifier.classify(executorServiceClaims, OIDC_ISSUER, client);
        assertEquals(CallerType.EXECUTOR_SERVICE, executorService.getCallerType());
        assertFalse(TerrakubeCallerClassifier.isAuthorizedForOrganization(executorService, ORG_ID, "", client));

        Map<String, Object> legacyTokenClaims = Map.of(
                "iss", "TerrakubeInternal",
                "sub", "TerrakubeInternal (TOKEN)"
        );
        CallerClassification legacy = TerrakubeCallerClassifier.classify(legacyTokenClaims, OIDC_ISSUER, client);
        assertEquals(CallerType.UNKNOWN_INTERNAL, legacy.getCallerType());
        assertFalse(TerrakubeCallerClassifier.isAuthorizedForOrganization(legacy, ORG_ID, "", client));

        Map<String, Object> registryWithWorkspaceClaims = Map.of(
                "iss", "TerrakubeInternal",
                "sub", "TerrakubeInternal (REGISTRY)",
                "workspaceId", WS_ID,
                "organizationId", ORG_ID
        );
        CallerClassification forgedRegistry = TerrakubeCallerClassifier.classify(registryWithWorkspaceClaims, OIDC_ISSUER, client);
        assertEquals(CallerType.UNKNOWN_INTERNAL, forgedRegistry.getCallerType());
        assertFalse(TerrakubeCallerClassifier.isAuthorizedForOrganization(forgedRegistry, ORG_ID, "", client));
    }

    @Test
    void workspaceExecutor_sameOrgAndActiveStep_isAuthorized() {
        stubStepStatus("running");

        Map<String, Object> claims = Map.of(
                "iss", "TerrakubeInternal",
                "sub", "TerrakubeInternal (EXECUTOR)",
                "organizationId", ORG_ID,
                "workspaceId", WS_ID,
                "jobId", "101",
                "stepId", STEP_UUID,
                "email", "owner@example.com",
                "groups", List.of("TERRAKUBE_ADMIN")
        );

        CallerClassification caller = TerrakubeCallerClassifier.classify(claims, OIDC_ISSUER, client);

        assertEquals(CallerType.WORKSPACE_EXECUTOR, caller.getCallerType());
        assertTrue(caller.getEffectiveGroups().isEmpty());
        assertEquals("", caller.getEmail());
        assertTrue(TerrakubeCallerClassifier.isAuthorizedForOrganization(caller, ORG_ID, "owner@example.com", client));
        assertTrue(TerrakubeCallerClassifier.isAuthorizedForOrganization(caller, ORG_ID.toUpperCase(), "", client));
    }

    @Test
    void workspaceExecutor_jtiFallback_resolvesJobAndStepIds() {
        stubStepStatus("pending");

        Map<String, Object> claims = Map.of(
                "iss", "TerrakubeInternal",
                "sub", "TerrakubeInternal (EXECUTOR)",
                "organizationId", ORG_ID,
                "workspaceId", WS_ID,
                "jti", "101-" + STEP_UUID
        );

        CallerClassification caller = TerrakubeCallerClassifier.classify(claims, OIDC_ISSUER, client);

        assertEquals(CallerType.WORKSPACE_EXECUTOR, caller.getCallerType());
        assertEquals("101", caller.getJobId());
        assertEquals(STEP_UUID, caller.getStepId());
        assertTrue(TerrakubeCallerClassifier.isAuthorizedForOrganization(caller, ORG_ID, "", client));
    }

    @Test
    void workspaceExecutor_crossOrg_isRejected() {
        Map<String, Object> claims = Map.of(
                "iss", "TerrakubeInternal",
                "sub", "TerrakubeInternal (EXECUTOR)",
                "organizationId", ORG_ID,
                "workspaceId", WS_ID,
                "jobId", "101",
                "stepId", STEP_UUID
        );

        CallerClassification caller = TerrakubeCallerClassifier.classify(claims, OIDC_ISSUER, client);

        assertEquals(CallerType.WORKSPACE_EXECUTOR, caller.getCallerType());
        assertFalse(TerrakubeCallerClassifier.isAuthorizedForOrganization(caller, OTHER_ORG_ID, "", client));
    }

    @Test
    void workspaceExecutor_missingOrganizationId_isRejected() {
        Map<String, Object> claims = Map.of(
                "iss", "TerrakubeInternal",
                "sub", "TerrakubeInternal (EXECUTOR)",
                "workspaceId", WS_ID,
                "jobId", "101",
                "stepId", STEP_UUID
        );

        CallerClassification caller = TerrakubeCallerClassifier.classify(claims, OIDC_ISSUER, client);

        assertEquals(CallerType.UNKNOWN_INTERNAL, caller.getCallerType());
        assertFalse(TerrakubeCallerClassifier.isAuthorizedForOrganization(caller, ORG_ID, "", client));
    }

    @Test
    void workspaceExecutor_terminalStepsAndNonUuidStep_areRejected() {
        Map<String, Object> claims = Map.of(
                "iss", "TerrakubeInternal",
                "sub", "TerrakubeInternal (EXECUTOR)",
                "organizationId", ORG_ID,
                "workspaceId", WS_ID,
                "jobId", "101",
                "stepId", STEP_UUID
        );
        CallerClassification caller = TerrakubeCallerClassifier.classify(claims, OIDC_ISSUER, client);

        for (String terminalStatus : List.of("completed", "failed", "cancelled", "noChanges", "rejected", "unknown")) {
            stubStepStatus(terminalStatus);
            assertFalse(
                    TerrakubeCallerClassifier.isAuthorizedForOrganization(caller, ORG_ID, "", client),
                    "Expected terminal/unknown step status '" + terminalStatus + "' to be rejected"
            );
        }

        CallerClassification invalidUuidCaller = CallerClassification.builder()
                .callerType(CallerType.WORKSPACE_EXECUTOR)
                .organizationId(ORG_ID)
                .workspaceId(WS_ID)
                .jobId("101")
                .stepId("not-a-uuid")
                .build();
        assertFalse(TerrakubeCallerClassifier.isAuthorizedForOrganization(invalidUuidCaller, ORG_ID, "", client));
    }

    @Test
    void workspaceExecutor_apiError_failsClosed() {
        stubFor(get(urlPathEqualTo("/api/v1/organization/" + ORG_ID + "/job/101/step/" + STEP_UUID))
                .willReturn(serverError()));

        Map<String, Object> claims = Map.of(
                "iss", "TerrakubeInternal",
                "sub", "TerrakubeInternal (EXECUTOR)",
                "organizationId", ORG_ID,
                "workspaceId", WS_ID,
                "jobId", "101",
                "stepId", STEP_UUID
        );

        CallerClassification caller = TerrakubeCallerClassifier.classify(claims, OIDC_ISSUER, client);
        assertFalse(TerrakubeCallerClassifier.isAuthorizedForOrganization(caller, ORG_ID, "", client));
    }

    // ──────────────────────────────── Federated OIDC Tokens ────────────────────────────────

    @Test
    void federated_matchingClaimsAndTeamInOrg_isAuthorized() {
        stubGitHubFederated("github-pipeline-team", "repository", "acme/infra");
        stubOrgTeams(ORG_ID, "github-pipeline-team");

        Map<String, Object> claims = Map.of(
                "iss", GITHUB_ISSUER,
                "aud", List.of("terrakube"),
                "sub", "repo:acme/infra:ref:refs/heads/main",
                "repository", "acme/infra",
                "email", "owner@example.com",
                "groups", List.of("TERRAKUBE_ADMIN")
        );

        CallerClassification caller = TerrakubeCallerClassifier.classify(claims, OIDC_ISSUER, client);

        assertEquals(CallerType.FEDERATED, caller.getCallerType());
        assertTrue(caller.isFederatedClaimsVerified());
        assertEquals("", caller.getEmail());
        assertEquals(java.util.Set.of("github-pipeline-team"), caller.getEffectiveGroups());
        assertTrue(TerrakubeCallerClassifier.isAuthorizedForOrganization(caller, ORG_ID, "owner@example.com", client));
    }

    @Test
    void federated_matchingClaimsAndWorkspaceAccessInOrg_isAuthorized() {
        stubGitHubFederated("github-ws-team", "repository", "acme/app");
        stubOrgTeams(ORG_ID);
        stubWorkspaceAccess(ORG_ID, "github-ws-team");

        Map<String, Object> claims = Map.of(
                "iss", GITHUB_ISSUER,
                "aud", "terrakube",
                "repository", "acme/app"
        );

        CallerClassification caller = TerrakubeCallerClassifier.classify(claims, OIDC_ISSUER, client);

        assertEquals(CallerType.FEDERATED, caller.getCallerType());
        assertTrue(caller.isFederatedClaimsVerified());
        assertTrue(TerrakubeCallerClassifier.isAuthorizedForOrganization(caller, ORG_ID, "", client));
    }

    @Test
    void federated_matchingClaimsInWrongOrg_isRejectedEvenWithSpoofedEmailAndGroups() {
        stubGitHubFederated("github-pipeline-team", "repository", "acme/infra");
        stubOrgTeams(OTHER_ORG_ID, "other-org-team");
        stubWorkspaceAccess(OTHER_ORG_ID, "other-ws-team");

        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", GITHUB_ISSUER);
        claims.put("aud", "terrakube");
        claims.put("repository", "acme/infra");
        claims.put("email", "owner@example.com");
        claims.put("groups", List.of("other-org-team", "owner@example.com"));

        CallerClassification caller = TerrakubeCallerClassifier.classify(claims, OIDC_ISSUER, client);

        assertEquals(CallerType.FEDERATED, caller.getCallerType());
        assertTrue(caller.isFederatedClaimsVerified());
        assertEquals(java.util.Set.of("github-pipeline-team"), caller.getEffectiveGroups());
        assertFalse(TerrakubeCallerClassifier.isAuthorizedForOrganization(
                caller, OTHER_ORG_ID, "owner@example.com", client));
    }

    @Test
    void federated_nonMatchingClaimOrApiError_failsClosed() {
        stubGitHubFederated("github-pipeline-team", "repository", "acme/infra");

        Map<String, Object> wrongRepoClaims = Map.of(
                "iss", GITHUB_ISSUER,
                "aud", "terrakube",
                "repository", "evil/repo",
                "email", "owner@example.com",
                "groups", List.of("github-pipeline-team")
        );

        CallerClassification rejected = TerrakubeCallerClassifier.classify(wrongRepoClaims, OIDC_ISSUER, client);
        assertEquals(CallerType.FEDERATED, rejected.getCallerType());
        assertFalse(rejected.isFederatedClaimsVerified());
        assertTrue(rejected.getEffectiveGroups().isEmpty());
        assertFalse(TerrakubeCallerClassifier.isAuthorizedForOrganization(
                rejected, ORG_ID, "owner@example.com", client));

        CallerClassification noClient = TerrakubeCallerClassifier.classify(wrongRepoClaims, OIDC_ISSUER, null);
        assertFalse(noClient.isFederatedClaimsVerified());
        assertFalse(TerrakubeCallerClassifier.isAuthorizedForOrganization(
                noClient, ORG_ID, "owner@example.com", client));

        stubFor(get(urlPathEqualTo("/api/v1/federated")).willReturn(serverError()));
        CallerClassification apiError = TerrakubeCallerClassifier.classify(wrongRepoClaims, OIDC_ISSUER, client);
        assertFalse(apiError.isFederatedClaimsVerified());
        assertFalse(TerrakubeCallerClassifier.isAuthorizedForOrganization(
                apiError, ORG_ID, "owner@example.com", client));
    }

    // ──────────────────────────────── PAT / OIDC User Tokens ────────────────────────────────

    @Test
    void patOrTeamToken_instanceOwnerTeamAccessAndNonMember() {
        stubOrgTeams(ORG_ID, "dev-team");
        stubWorkspaceAccess(ORG_ID, "ws-limited-team");

        CallerClassification ownerCaller = TerrakubeCallerClassifier.classify(
                Map.of("iss", "Terrakube", "sub", "owner", "email", "owner@example.com"),
                OIDC_ISSUER, client);
        assertEquals(CallerType.PAT_OR_TEAM_TOKEN, ownerCaller.getCallerType());
        assertTrue(TerrakubeCallerClassifier.isAuthorizedForOrganization(
                ownerCaller, ORG_ID, "owner@example.com", client));

        CallerClassification teamCaller = TerrakubeCallerClassifier.classify(
                Map.of("iss", "Terrakube", "sub", "dev (Team Token)", "groups", List.of("dev-team")),
                OIDC_ISSUER, client);
        assertEquals(CallerType.PAT_OR_TEAM_TOKEN, teamCaller.getCallerType());
        assertTrue(TerrakubeCallerClassifier.isAuthorizedForOrganization(
                teamCaller, ORG_ID, "owner@example.com", client));

        CallerClassification accessCaller = TerrakubeCallerClassifier.classify(
                Map.of("iss", "Terrakube", "sub", "ws (Team Token)", "groups", "ws-limited-team"),
                OIDC_ISSUER, client);
        assertTrue(TerrakubeCallerClassifier.isAuthorizedForOrganization(
                accessCaller, ORG_ID, "owner@example.com", client));

        CallerClassification outsider = TerrakubeCallerClassifier.classify(
                Map.of("iss", "Terrakube", "sub", "outsider", "email", "other@example.com", "groups", List.of("other-team")),
                OIDC_ISSUER, client);
        assertFalse(TerrakubeCallerClassifier.isAuthorizedForOrganization(
                outsider, ORG_ID, "owner@example.com", client));
    }

    @Test
    void oidcUser_instanceOwnerTeamAccessAndNonMember() {
        stubOrgTeams(ORG_ID, "platform-team");
        stubWorkspaceAccess(ORG_ID, "ws-readers");

        CallerClassification ownerUser = TerrakubeCallerClassifier.classify(
                Map.of("iss", OIDC_ISSUER, "sub", "user-1", "email", "owner@example.com", "groups", List.of()),
                OIDC_ISSUER, client);
        assertEquals(CallerType.OIDC_USER, ownerUser.getCallerType());
        assertTrue(TerrakubeCallerClassifier.isAuthorizedForOrganization(
                ownerUser, ORG_ID, "owner@example.com", client));

        CallerClassification teamUser = TerrakubeCallerClassifier.classify(
                Map.of("iss", OIDC_ISSUER, "sub", "user-2", "email", "alice@example.com", "groups", List.of("platform-team")),
                OIDC_ISSUER, client);
        assertTrue(TerrakubeCallerClassifier.isAuthorizedForOrganization(
                teamUser, ORG_ID, "owner@example.com", client));

        CallerClassification limitedUser = TerrakubeCallerClassifier.classify(
                Map.of("iss", OIDC_ISSUER, "sub", "user-3", "email", "bob@example.com", "groups", List.of("ws-readers")),
                OIDC_ISSUER, client);
        assertTrue(TerrakubeCallerClassifier.isAuthorizedForOrganization(
                limitedUser, ORG_ID, "owner@example.com", client));

        CallerClassification unauthorizedUser = TerrakubeCallerClassifier.classify(
                Map.of("iss", OIDC_ISSUER, "sub", "user-4", "email", "mallory@example.com", "groups", List.of("strangers")),
                OIDC_ISSUER, client);
        assertFalse(TerrakubeCallerClassifier.isAuthorizedForOrganization(
                unauthorizedUser, ORG_ID, "owner@example.com", client));
    }

    // ──────────────────────────────── Helpers ────────────────────────────────

    private void stubStepStatus(String status) {
        stubFor(get(urlPathEqualTo("/api/v1/organization/" + ORG_ID + "/job/101/step/" + STEP_UUID))
                .willReturn(okJson(
                        "{\"data\":{\"id\":\"" + STEP_UUID + "\",\"type\":\"step\","
                        + "\"attributes\":{\"stepNumber\":\"1\",\"status\":\"" + status + "\"}}}"
                )));
    }

    private void stubGitHubFederated(String federatedName, String claimKey, String claimValue) {
        stubFor(get(urlPathEqualTo("/api/v1/federated"))
                .withQueryParam("include", equalTo("claims"))
                .willReturn(okJson(
                        "{\"data\":[{\"id\":\"fed-1\",\"type\":\"federated\","
                        + "\"attributes\":{\"name\":\"" + federatedName + "\","
                        + "\"issuerUrl\":\"" + GITHUB_ISSUER + "\",\"audience\":\"terrakube\"},"
                        + "\"relationships\":{\"claims\":{\"data\":[{\"id\":\"claim-1\",\"type\":\"federated_claim\"}]}}}],"
                        + "\"included\":[{\"id\":\"claim-1\",\"type\":\"federated_claim\","
                        + "\"attributes\":{\"claimKey\":\"" + claimKey + "\",\"claimValue\":\"" + claimValue + "\"}}]}"
                )));
    }

    private void stubOrgTeams(String orgId, String... teamNames) {
        StringBuilder data = new StringBuilder("[");
        for (int i = 0; i < teamNames.length; i++) {
            if (i > 0) {
                data.append(",");
            }
            data.append("{\"id\":\"team-").append(i).append("\",\"type\":\"team\",")
                    .append("\"attributes\":{\"name\":\"").append(teamNames[i]).append("\",\"role\":\"read\"}}");
        }
        data.append("]");
        stubFor(get(urlPathEqualTo("/api/v1/organization/" + orgId + "/team"))
                .willReturn(okJson("{\"data\":" + data + "}")));
    }

    private void stubWorkspaceAccess(String orgId, String... accessNames) {
        StringBuilder included = new StringBuilder("[");
        for (int i = 0; i < accessNames.length; i++) {
            if (i > 0) {
                included.append(",");
            }
            included.append("{\"id\":\"acc-").append(i).append("\",\"type\":\"access\",")
                    .append("\"attributes\":{\"name\":\"").append(accessNames[i]).append("\",\"role\":\"read\"}}");
        }
        included.append("]");
        stubFor(get(urlPathEqualTo("/api/v1/organization/" + orgId + "/workspace"))
                .withQueryParam("include", equalTo("access"))
                .willReturn(okJson("{\"data\":[],\"included\":" + included + "}")));
    }
}
