package io.terrakube.client;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import feign.Feign;
import feign.http2client.Http2Client;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import io.terrakube.client.model.graphql.GraphQLRequest;
import io.terrakube.client.model.graphql.GraphQLResponse;
import io.terrakube.client.model.graphql.queries.search.module.SearchOrganizationModuleResponse;
import io.terrakube.client.model.graphql.queries.search.organization.SearchOrganizationResponse;
import io.terrakube.client.model.graphql.queries.search.provider.SearchOrganizationProviderResponse;
import io.terrakube.client.model.organization.Organization;
import io.terrakube.client.model.organization.job.Job;
import io.terrakube.client.model.organization.job.JobAttributes;
import io.terrakube.client.model.organization.job.JobRequest;
import io.terrakube.client.model.organization.job.LogsRequest;
import io.terrakube.client.model.organization.job.step.Step;
import io.terrakube.client.model.organization.job.step.StepAttributes;
import io.terrakube.client.model.organization.job.step.StepRequest;
import io.terrakube.client.model.organization.module.Module;
import io.terrakube.client.model.organization.module.ModuleAttributes;
import io.terrakube.client.model.organization.module.ModuleRequest;
import io.terrakube.client.model.organization.module.version.ModuleVersion;
import io.terrakube.client.model.organization.provider.version.Version;
import io.terrakube.client.model.organization.provider.version.implementation.Implementation;
import io.terrakube.client.model.organization.ssh.Ssh;
import io.terrakube.client.model.organization.template.Template;
import io.terrakube.client.model.organization.vcs.Vcs;
import io.terrakube.client.model.organization.vcs.github_app_token.GitHubAppToken;
import io.terrakube.client.model.organization.workspace.Workspace;
import io.terrakube.client.model.organization.workspace.history.HistoryRequest;
import io.terrakube.client.model.organization.workspace.variable.Variable;
import io.terrakube.client.model.refresh.RefreshTokenRequest;
import io.terrakube.client.model.response.Response;
import io.terrakube.client.model.response.ResponseWithInclude;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@WireMockTest
class TerrakubeClientTest {

    private TerrakubeClient client;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        client = Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .client(new Http2Client())
                .target(TerrakubeClient.class, wmRuntimeInfo.getHttpBaseUrl());
    }

    // ──────────────────────────────── Organization ────────────────────────────────

    @Test
    void getAllOrganizations_returnsOrganizationList() {
        stubFor(get(urlPathEqualTo("/api/v1/organization"))
                .willReturn(okJson(
                        "{\"data\":[{\"id\":\"org-1\",\"type\":\"organization\","
                        + "\"attributes\":{\"name\":\"acme\",\"description\":\"Acme Corp\"}}]}"
                )));

        Response<List<Organization>> response = client.getAllOrganizations();

        assertNotNull(response);
        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        Organization org = response.getData().get(0);
        assertEquals("org-1", org.getId());
        assertEquals("organization", org.getType());
        assertEquals("acme", org.getAttributes().getName());
        assertEquals("Acme Corp", org.getAttributes().getDescription());
    }

    @Test
    void getOrganizationByName_returnsMatchingOrganization() {
        stubFor(get(urlPathEqualTo("/api/v1/organization"))
                .willReturn(okJson(
                        "{\"data\":[{\"id\":\"org-2\",\"type\":\"organization\","
                        + "\"attributes\":{\"name\":\"hashicorp\"}}]}"
                )));

        Response<List<Organization>> response = client.getOrganizationByName("hashicorp");

        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertEquals("hashicorp", response.getData().get(0).getAttributes().getName());
    }

    @Test
    void getAllOrganizationsWithJobStatus_returnsOrgsAndIncludedJobs() {
        stubFor(get(urlPathEqualTo("/api/v1/organization"))
                .willReturn(okJson(
                        "{\"data\":[{\"id\":\"org-1\",\"type\":\"organization\","
                        + "\"attributes\":{\"name\":\"acme\"}}],"
                        + "\"included\":[{\"id\":\"job-1\",\"type\":\"job\","
                        + "\"attributes\":{\"status\":\"pending\",\"output\":\"\"}}]}"
                )));

        ResponseWithInclude<List<Organization>, Job> response =
                client.getAllOrganizationsWithJobStatus("pending");

        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertNotNull(response.getIncluded());
        assertEquals(1, response.getIncluded().size());
        assertEquals("job-1", response.getIncluded().get(0).getId());
        assertEquals("pending", response.getIncluded().get(0).getAttributes().getStatus());
    }

    @Test
    void getOrganizationsByNameAndProvider_returnsMatchingOrganization() {
        stubFor(get(urlPathEqualTo("/api/v1/organization"))
                .willReturn(okJson(
                        "{\"data\":[{\"id\":\"org-3\",\"type\":\"organization\","
                        + "\"attributes\":{\"name\":\"sampleOrg\"}}]}"
                )));

        Response<List<Organization>> response =
                client.getOrganizationsByNameAndProvider("sampleOrg", "aws");

        assertNotNull(response.getData());
        assertEquals("org-3", response.getData().get(0).getId());
    }

    // ──────────────────────────────── Workspace ────────────────────────────────

    @Test
    void getAllWorkspaces_returnsWorkspaceList() {
        stubFor(get(urlPathEqualTo("/api/v1/organization/org-1/workspace"))
                .willReturn(okJson(
                        "{\"data\":[{\"id\":\"ws-1\",\"type\":\"workspace\","
                        + "\"attributes\":{\"name\":\"my-workspace\"}}]}"
                )));

        Response<List<Workspace>> response = client.getAllWorkspaces("org-1");

        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertEquals("ws-1", response.getData().get(0).getId());
        assertEquals("my-workspace", response.getData().get(0).getAttributes().getName());
    }

    @Test
    void getWorkspaceById_returnsSingleWorkspace() {
        stubFor(get(urlPathEqualTo("/api/v1/organization/org-1/workspace/ws-1"))
                .willReturn(okJson(
                        "{\"data\":{\"id\":\"ws-1\",\"type\":\"workspace\","
                        + "\"attributes\":{\"name\":\"my-workspace\"}}}"
                )));

        Response<Workspace> response = client.getWorkspaceById("org-1", "ws-1");

        assertNotNull(response.getData());
        assertEquals("ws-1", response.getData().getId());
        assertEquals("my-workspace", response.getData().getAttributes().getName());
    }

    @Test
    void getWorkspaceByIdWithVariables_includesVariables() {
        stubFor(get(urlPathEqualTo("/api/v1/organization/org-1/workspace/ws-1"))
                .willReturn(okJson(
                        "{\"data\":{\"id\":\"ws-1\",\"type\":\"workspace\","
                        + "\"attributes\":{\"name\":\"ws\"}},"
                        + "\"included\":[{\"id\":\"var-1\",\"type\":\"variable\","
                        + "\"attributes\":{\"key\":\"TF_VAR_region\",\"value\":\"us-east-1\","
                        + "\"sensitive\":false,\"category\":\"terraform\"}}]}"
                )));

        ResponseWithInclude<Workspace, Variable> response =
                client.getWorkspaceByIdWithVariables("org-1", "ws-1");

        assertNotNull(response.getData());
        assertEquals("ws-1", response.getData().getId());
        assertNotNull(response.getIncluded());
        assertEquals(1, response.getIncluded().size());
        Variable var = response.getIncluded().get(0);
        assertEquals("var-1", var.getId());
        assertEquals("TF_VAR_region", var.getAttributes().getKey());
        assertEquals("us-east-1", var.getAttributes().getValue());
    }

    // ──────────────────────────────── Variables ────────────────────────────────

    @Test
    void getAllVariables_returnsTerraformVariables() {
        stubFor(get(urlPathEqualTo("/api/v1/organization/org-1/workspace/ws-1/variable"))
                .willReturn(okJson(
                        "{\"data\":[{\"id\":\"var-1\",\"type\":\"variable\","
                        + "\"attributes\":{\"key\":\"region\",\"value\":\"eu-west-1\","
                        + "\"sensitive\":false,\"category\":\"terraform\"}}]}"
                )));

        Response<List<Variable>> response = client.getAllVariables("org-1", "ws-1");

        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertEquals("region", response.getData().get(0).getAttributes().getKey());
        assertEquals("eu-west-1", response.getData().get(0).getAttributes().getValue());
        assertFalse(response.getData().get(0).getAttributes().isSensitive());
    }

    @Test
    void getAllSecrets_returnsSensitiveVariables() {
        stubFor(get(urlPathEqualTo("/api/v1/organization/org-1/workspace/ws-1/variable"))
                .willReturn(okJson(
                        "{\"data\":[{\"id\":\"sec-1\",\"type\":\"variable\","
                        + "\"attributes\":{\"key\":\"AWS_SECRET_KEY\",\"value\":\"secret\","
                        + "\"sensitive\":true,\"category\":\"terraform\"}}]}"
                )));

        Response<List<Variable>> response = client.getAllSecrets("org-1", "ws-1");

        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertTrue(response.getData().get(0).getAttributes().isSensitive());
        assertEquals("AWS_SECRET_KEY", response.getData().get(0).getAttributes().getKey());
    }

    @Test
    void getAllEnvironmentVariables_returnsEnvCategoryVariables() {
        stubFor(get(urlPathEqualTo("/api/v1/organization/org-1/workspace/ws-1/variable"))
                .willReturn(okJson(
                        "{\"data\":[{\"id\":\"env-1\",\"type\":\"variable\","
                        + "\"attributes\":{\"key\":\"TF_LOG\",\"value\":\"DEBUG\","
                        + "\"sensitive\":false,\"category\":\"env\"}}]}"
                )));

        Response<List<Variable>> response = client.getAllEnvironmentVariables("org-1", "ws-1");

        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertEquals("env", response.getData().get(0).getAttributes().getCategory());
        assertEquals("TF_LOG", response.getData().get(0).getAttributes().getKey());
    }

    // ──────────────────────────────── Job / Step ────────────────────────────────

    @Test
    void getJobById_returnsJobWithIncludedSteps() {
        stubFor(get(urlPathEqualTo("/api/v1/organization/org-1/job/job-1"))
                .willReturn(okJson(
                        "{\"data\":{\"id\":\"job-1\",\"type\":\"job\","
                        + "\"attributes\":{\"status\":\"running\",\"tcl\":\"step1\",\"overrideSource\":\"override\",\"planChanges\":false}},"
                        + "\"included\":[{\"id\":\"step-1\",\"type\":\"step\","
                        + "\"attributes\":{\"stepNumber\":\"1\",\"status\":\"running\"}}]}"
                )));

        ResponseWithInclude<Job, Step> response = client.getJobById("org-1", "job-1");

        assertNotNull(response.getData());
        assertEquals("job-1", response.getData().getId());
        assertEquals("running", response.getData().getAttributes().getStatus());
        assertEquals("override", response.getData().getAttributes().getOverrideSource());
        assertEquals("step1", response.getData().getAttributes().getTcl());
        assertFalse(response.getData().getAttributes().isPlanChanges());

        assertNotNull(response.getIncluded());
        assertEquals(1, response.getIncluded().size());
        assertEquals("step-1", response.getIncluded().get(0).getId());
        assertEquals("1", response.getIncluded().get(0).getAttributes().getStepNumber());
    }

    @Test
    void updateJob_sendsCorrectPatchRequest() {
        stubFor(patch(urlPathEqualTo("/api/v1/organization/org-1/job/job-1"))
                .willReturn(noContent()));

        Job job = new Job();
        job.setId("job-1");
        job.setType("job");
        JobAttributes attrs = new JobAttributes();
        attrs.setStatus("completed");
        attrs.setApprovalTeam("");
        job.setAttributes(attrs);

        JobRequest jobRequest = new JobRequest();
        jobRequest.setData(job);

        assertDoesNotThrow(() -> client.updateJob(jobRequest, "org-1", "job-1"));

        verify(patchRequestedFor(urlPathEqualTo("/api/v1/organization/org-1/job/job-1"))
                .withHeader("Content-Type", containing("application/vnd.api+json")));
    }

    @Test
    void createStep_sendsPostToStepEndpoint() {
        stubFor(post(urlPathEqualTo("/api/v1/organization/org-1/job/job-1/step"))
                .willReturn(created()));

        Step step = new Step();
        step.setType("step");
        StepAttributes attrs = new StepAttributes();
        attrs.setStepNumber("1");
        attrs.setStatus("running");
        attrs.setOutput("");
        step.setAttributes(attrs);

        StepRequest stepRequest = new StepRequest();
        stepRequest.setData(step);

        assertDoesNotThrow(() -> client.createStep(stepRequest, "org-1", "job-1"));

        verify(postRequestedFor(urlPathEqualTo("/api/v1/organization/org-1/job/job-1/step"))
                .withHeader("Content-Type", containing("application/vnd.api+json")));
    }

    @Test
    void updateStep_sendsPatchToStepEndpoint() {
        stubFor(patch(urlPathEqualTo("/api/v1/organization/org-1/job/job-1/step/step-1"))
                .willReturn(noContent()));

        Step step = new Step();
        step.setId("step-1");
        step.setType("step");
        StepAttributes attrs = new StepAttributes();
        attrs.setStatus("completed");
        attrs.setOutput("Hello World");
        step.setAttributes(attrs);

        StepRequest stepRequest = new StepRequest();
        stepRequest.setData(step);

        assertDoesNotThrow(() -> client.updateStep(stepRequest, "org-1", "job-1", "step-1"));

        verify(patchRequestedFor(urlPathEqualTo("/api/v1/organization/org-1/job/job-1/step/step-1"))
                .withHeader("Content-Type", containing("application/vnd.api+json")));
    }

    @Test
    void appendLogs_sendsPostToLogsEndpoint() {
        stubFor(post(urlPathEqualTo("/logs"))
                .willReturn(noContent()));

        LogsRequest logsRequest = new LogsRequest();

        assertDoesNotThrow(() -> client.appendLogs(logsRequest));

        verify(postRequestedFor(urlPathEqualTo("/logs"))
                .withHeader("Content-Type", containing("application/vnd.api+json")));
    }

    @Test
    void setupConsumerGroups_sendsPostToConsumerGroupsEndpoint() {
        stubFor(post(urlPathEqualTo("/logs/job-1/setup-consumer-groups"))
                .willReturn(noContent()));

        assertDoesNotThrow(() -> client.setupConsumerGroups("job-1"));

        verify(postRequestedFor(urlPathEqualTo("/logs/job-1/setup-consumer-groups")));
    }

    // ──────────────────────────────── History ────────────────────────────────

    @Test
    void createHistory_sendsPostToHistoryEndpoint() {
        stubFor(post(urlPathEqualTo("/api/v1/organization/org-1/workspace/ws-1/history"))
                .willReturn(created()));

        HistoryRequest historyRequest = new HistoryRequest();

        assertDoesNotThrow(() -> client.createHistory(historyRequest, "org-1", "ws-1"));

        verify(postRequestedFor(urlPathEqualTo("/api/v1/organization/org-1/workspace/ws-1/history"))
                .withHeader("Content-Type", containing("application/vnd.api+json")));
    }

    // ──────────────────────────────── Module ────────────────────────────────

    @Test
    void getModuleById_returnsModule() {
        stubFor(get(urlPathEqualTo("/api/v1/organization/org-1/module/mod-1"))
                .willReturn(okJson(
                        "{\"data\":{\"id\":\"mod-1\",\"type\":\"module\","
                        + "\"attributes\":{\"name\":\"vpc\",\"provider\":\"aws\",\"downloadQuantity\":42}}}"
                )));

        Response<Module> response = client.getModuleById("org-1", "mod-1");

        assertNotNull(response.getData());
        assertEquals("mod-1", response.getData().getId());
        assertEquals("vpc", response.getData().getAttributes().getName());
        assertEquals("aws", response.getData().getAttributes().getProvider());
        assertEquals(42, response.getData().getAttributes().getDownloadQuantity());
    }

    @Test
    void getModuleByNameAndProvider_returnsFilteredModules() {
        stubFor(get(urlPathEqualTo("/api/v1/organization/org-1/module"))
                .willReturn(okJson(
                        "{\"data\":[{\"id\":\"mod-1\",\"type\":\"module\","
                        + "\"attributes\":{\"name\":\"vpc\",\"provider\":\"aws\"}}]}"
                )));

        Response<List<Module>> response = client.getModuleByNameAndProvider("org-1", "vpc", "aws");

        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertEquals("vpc", response.getData().get(0).getAttributes().getName());
    }

    @Test
    void updateModule_incrementsDownloadQuantityViaPatch() {
        stubFor(patch(urlPathEqualTo("/api/v1/organization/org-1/module/mod-1"))
                .willReturn(noContent()));

        Module module = new Module();
        module.setId("mod-1");
        module.setType("module");
        ModuleAttributes attrs = new ModuleAttributes();
        attrs.setDownloadQuantity(43);
        module.setAttributes(attrs);

        ModuleRequest moduleRequest = new ModuleRequest();
        moduleRequest.setData(module);

        assertDoesNotThrow(() -> client.updateModule(moduleRequest, "org-1", "mod-1"));

        verify(patchRequestedFor(urlPathEqualTo("/api/v1/organization/org-1/module/mod-1"))
                .withHeader("Content-Type", containing("application/vnd.api+json")));
    }

    @Test
    void getAllVersionsByOrganizationIdAndModuleId_returnsVersionList() {
        stubFor(get(urlPathEqualTo("/api/v1/organization/org-1/module/mod-1/version"))
                .willReturn(okJson(
                        "{\"data\":[{\"id\":\"ver-1\",\"type\":\"version\","
                        + "\"attributes\":{\"version\":\"1.0.0\"}}]}"
                )));

        Response<List<ModuleVersion>> response =
                client.getAllVersionsByOrganizationIdAndModuleId("org-1", "mod-1");

        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertEquals("1.0.0", response.getData().get(0).getAttributes().getVersion());
    }

    // ──────────────────────────────── Provider / Version ────────────────────────────────

    @Test
    void getAllVersionsByProviderWithImplementation_returnsVersionsAndImplementations() {
        stubFor(get(urlPathEqualTo("/api/v1/organization/org-1/provider/prov-1/version"))
                .willReturn(okJson(
                        "{\"data\":[{\"id\":\"ver-1\",\"type\":\"version\","
                        + "\"attributes\":{\"versionNumber\":\"2.0.0\",\"protocols\":\"[5.0]\"}}],"
                        + "\"included\":[{\"id\":\"impl-1\",\"type\":\"implementation\","
                        + "\"attributes\":{\"os\":\"linux\",\"arch\":\"amd64\","
                        + "\"filename\":\"provider_2.0.0_linux_amd64.zip\"}}]}"
                )));

        ResponseWithInclude<List<Version>, Implementation> response =
                client.getAllVersionsByProviderWithImplementation("org-1", "prov-1");

        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertEquals("2.0.0", response.getData().get(0).getAttributes().getVersionNumber());

        assertNotNull(response.getIncluded());
        assertEquals(1, response.getIncluded().size());
        assertEquals("linux", response.getIncluded().get(0).getAttributes().getOs());
        assertEquals("amd64", response.getIncluded().get(0).getAttributes().getArch());
    }

    @Test
    void getVersionsByProviderIdAndVersionNumber_returnsMatchingVersion() {
        stubFor(get(urlPathEqualTo("/api/v1/organization/org-1/provider/prov-1/version"))
                .willReturn(okJson(
                        "{\"data\":[{\"id\":\"ver-1\",\"type\":\"version\","
                        + "\"attributes\":{\"versionNumber\":\"2.0.0\"}}]}"
                )));

        Response<List<Version>> response =
                client.getVersionsByOrganizationIdAndProviderIdAndVersionNumber(
                        "org-1", "prov-1", "2.0.0");

        assertNotNull(response.getData());
        assertEquals("2.0.0", response.getData().get(0).getAttributes().getVersionNumber());
    }

    @Test
    void getImplementationByOsArchVersion_returnsFilteredImplementation() {
        stubFor(get(urlPathEqualTo(
                "/api/v1/organization/org-1/provider/prov-1/version/ver-1/implementation"))
                .willReturn(okJson(
                        "{\"data\":[{\"id\":\"impl-1\",\"type\":\"implementation\","
                        + "\"attributes\":{\"os\":\"darwin\",\"arch\":\"amd64\","
                        + "\"filename\":\"provider_1.0.0_darwin_amd64.zip\"}}]}"
                )));

        Response<List<Implementation>> response =
                client.getImplementationByOsArchVersion(
                        "org-1", "prov-1", "ver-1", "darwin", "amd64");

        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertEquals("darwin", response.getData().get(0).getAttributes().getOs());
        assertEquals("amd64", response.getData().get(0).getAttributes().getArch());
        assertEquals("provider_1.0.0_darwin_amd64.zip",
                response.getData().get(0).getAttributes().getFilename());
    }

    // ──────────────────────────────── VCS / SSH / Template ────────────────────────────────

    @Test
    void getVcsById_returnsVcs() {
        stubFor(get(urlPathEqualTo("/api/v1/organization/org-1/vcs/vcs-1"))
                .willReturn(okJson(
                        "{\"data\":{\"id\":\"vcs-1\",\"type\":\"vcs\","
                        + "\"attributes\":{\"name\":\"github\",\"clientId\":\"client-123\"}}}"
                )));

        Response<Vcs> response = client.getVcsById("org-1", "vcs-1");

        assertNotNull(response.getData());
        assertEquals("vcs-1", response.getData().getId());
        assertEquals("github", response.getData().getAttributes().getName());
        assertEquals("client-123", response.getData().getAttributes().getClientId());
    }

    @Test
    void getSshById_returnsSsh() {
        stubFor(get(urlPathEqualTo("/api/v1/organization/org-1/ssh/ssh-1"))
                .willReturn(okJson(
                        "{\"data\":{\"id\":\"ssh-1\",\"type\":\"ssh\","
                        + "\"attributes\":{\"name\":\"deploy-key\",\"sshType\":\"rsa\"}}}"
                )));

        Response<Ssh> response = client.getSshById("org-1", "ssh-1");

        assertNotNull(response.getData());
        assertEquals("ssh-1", response.getData().getId());
        assertEquals("deploy-key", response.getData().getAttributes().getName());
        assertEquals("rsa", response.getData().getAttributes().getSshType());
    }

    @Test
    void getTemplateById_returnsTemplate() {
        stubFor(get(urlPathEqualTo("/api/v1/organization/org-1/template/tmpl-1"))
                .willReturn(okJson(
                        "{\"data\":{\"id\":\"tmpl-1\",\"type\":\"template\","
                        + "\"attributes\":{\"name\":\"plan-apply\",\"description\":\"Default plan/apply\"}}}"
                )));

        Response<Template> response = client.getTemplateById("org-1", "tmpl-1");

        assertNotNull(response.getData());
        assertEquals("tmpl-1", response.getData().getId());
        assertEquals("plan-apply", response.getData().getAttributes().getName());
        assertEquals("Default plan/apply", response.getData().getAttributes().getDescription());
    }

    @Test
    void getGitHubAppTokenByVcsIdAndOwner_returnsTokens() {
        stubFor(get(urlPathEqualTo("/api/v1/github_app_token"))
                .willReturn(okJson(
                        "{\"data\":[{\"id\":\"token-1\",\"type\":\"github_app_token\","
                        + "\"attributes\":{\"token\":\"ghp_abc123\",\"owner\":\"myorg\"}}]}"
                )));

        Response<List<GitHubAppToken>> response =
                client.getGitHubAppTokenByVcsIdAndOwner("myorg", "app-42");

        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertEquals("myorg", response.getData().get(0).getAttributes().getOwner());
        assertEquals("ghp_abc123", response.getData().get(0).getAttributes().getToken());
    }

    // ──────────────────────────────── GraphQL ────────────────────────────────

    @Test
    void searchOrganization_returnsOrganizationEdges() {
        stubFor(post(urlPathEqualTo("/graphql/api/v1"))
                .willReturn(okJson(
                        "{\"data\":{\"organization\":{\"edges\":"
                        + "[{\"node\":{\"id\":\"org-1\",\"name\":\"acme\"}}]}}}"
                )));

        GraphQLRequest request = new GraphQLRequest();
        request.setQuery("{ organization { edges { node { id name } } } }");

        GraphQLResponse<SearchOrganizationResponse> response = client.searchOrganization(request);

        assertNotNull(response.getData());
        assertNotNull(response.getData().getOrganization());
        assertEquals(1, response.getData().getOrganization().getEdges().size());
        assertEquals("org-1",
                response.getData().getOrganization().getEdges().get(0).getNode().getId());
        assertEquals("acme",
                response.getData().getOrganization().getEdges().get(0).getNode().getName());
    }

    @Test
    void searchOrganizationModules_returnsModuleEdges() {
        stubFor(post(urlPathEqualTo("/graphql/api/v1"))
                .willReturn(okJson(
                        "{\"data\":{\"organization\":{\"edges\":[{\"node\":"
                        + "{\"id\":\"org-1\",\"name\":\"acme\",\"module\":{\"edges\":"
                        + "[{\"node\":{\"id\":\"mod-1\",\"name\":\"vpc\",\"provider\":\"aws\","
                        + "\"version\":{\"edges\":[]}}}]}}}]}}}"
                )));

        GraphQLRequest request = new GraphQLRequest();
        request.setQuery("{ organization { edges { node { id name module { edges { node { id } } } } } } }");

        GraphQLResponse<SearchOrganizationModuleResponse> response =
                client.searchOrganizationModules(request);

        assertNotNull(response.getData());
        assertEquals(1, response.getData().getOrganization().getEdges().size());
        assertEquals("mod-1",
                response.getData().getOrganization().getEdges().get(0)
                        .getNode().getModule().getEdges().get(0).getNode().getId());
    }

    @Test
    void searchOrganizationProviders_returnsProviderEdges() {
        stubFor(post(urlPathEqualTo("/graphql/api/v1"))
                .willReturn(okJson(
                        "{\"data\":{\"organization\":{\"edges\":[{\"node\":"
                        + "{\"id\":\"org-1\",\"name\":\"acme\",\"provider\":{\"edges\":"
                        + "[{\"node\":{\"id\":\"prov-1\",\"name\":\"random\","
                        + "\"version\":{\"edges\":[]}}}]}}}]}}}"
                )));

        GraphQLRequest request = new GraphQLRequest();
        request.setQuery("{ organization { edges { node { id name provider { edges { node { id } } } } } } }");

        GraphQLResponse<SearchOrganizationProviderResponse> response =
                client.searchOrganizationProviders(request);

        assertNotNull(response.getData());
        assertEquals("prov-1",
                response.getData().getOrganization().getEdges().get(0)
                        .getNode().getProvider().getEdges().get(0).getNode().getId());
    }

    // ──────────────────────────────── Token Refresh ────────────────────────────────

    @Test
    void refreshToken_sendsPostToRefreshEndpointWithVcsId() {
        stubFor(post(urlPathEqualTo("/refresh-token/v1/vcs/vcs-1"))
                .willReturn(noContent()));

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setGitPath("https://github.com/myorg/myrepo.git");

        assertDoesNotThrow(() -> client.refreshToken("vcs-1", request));

        verify(postRequestedFor(urlPathEqualTo("/refresh-token/v1/vcs/vcs-1"))
                .withHeader("Content-Type", containing("application/json")));
    }

    @Test
    void refreshToken_includesGitPathInRequestBody() {
        stubFor(post(urlPathEqualTo("/refresh-token/v1/vcs/vcs-2"))
                .willReturn(noContent()));

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setGitPath("https://github.com/alfespa17/terraform-azurerm-random.git");

        client.refreshToken("vcs-2", request);

        verify(postRequestedFor(urlPathEqualTo("/refresh-token/v1/vcs/vcs-2"))
                .withRequestBody(containing("terraform-azurerm-random")));
    }

    @Test
    void refreshToken_usesVcsIdAsPathParameter() {
        stubFor(post(urlPathEqualTo("/refresh-token/v1/vcs/7163c5b1-0cd7-4820-9359-786aa427588d"))
                .willReturn(noContent()));

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setGitPath("https://github.com/alfespa17/terraform-azurerm-random.git");

        assertDoesNotThrow(() ->
                client.refreshToken("7163c5b1-0cd7-4820-9359-786aa427588d", request));

        verify(postRequestedFor(
                urlPathEqualTo("/refresh-token/v1/vcs/7163c5b1-0cd7-4820-9359-786aa427588d")));
    }
}
