package io.terrakube.client;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import io.terrakube.client.model.graphql.GraphQLRequest;
import io.terrakube.client.model.graphql.GraphQLResponse;
import io.terrakube.client.model.graphql.queries.search.module.SearchOrganizationModuleResponse;
import io.terrakube.client.model.graphql.queries.search.organization.SearchOrganizationResponse;
import io.terrakube.client.model.graphql.queries.search.provider.SearchOrganizationProviderResponse;
import io.terrakube.client.model.organization.Organization;
import io.terrakube.client.model.organization.job.Job;
import io.terrakube.client.model.organization.job.JobRequest;
import io.terrakube.client.model.organization.job.step.Step;
import io.terrakube.client.model.organization.job.step.StepRequest;
import io.terrakube.client.model.organization.job.LogsRequest;
import io.terrakube.client.model.organization.module.Module;
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
import io.terrakube.client.model.response.Response;
import io.terrakube.client.model.response.ResponseWithInclude;

import java.util.List;

public interface TerrakubeClient {

    @RequestLine("GET /api/v1/organization")
    Response<List<Organization>> getAllOrganizations();

    @RequestLine("GET /api/v1/organization?filter[job]=status=={jobStatus}&include=job")
    ResponseWithInclude<List<Organization>, Job> getAllOrganizationsWithJobStatus(@Param("jobStatus") String jobStatus);

    @RequestLine("GET /api/v1/organization/{organizationId}/workspace/{workspaceId}/variable?filter[variable]=sensitive==true;category==terraform")
    Response<List<Variable>> getAllSecrets(@Param("organizationId") String organizationId, @Param("workspaceId") String workspaceId);

    @RequestLine("GET /api/v1/organization/{organizationId}/workspace/{workspaceId}/variable?filter[variable]=sensitive==false;category==terraform")
    Response<List<Variable>> getAllVariables(@Param("organizationId") String organizationId, @Param("workspaceId") String workspaceId);

    @RequestLine("GET /api/v1/organization/{organizationId}/workspace/{workspaceId}/variable?filter[variable]=category==env")
    Response<List<Variable>> getAllEnvironmentVariables(@Param("organizationId") String organizationId, @Param("workspaceId") String workspaceId);

    @RequestLine("GET /api/v1/organization/{organizationId}/workspace")
    Response<List<Workspace>> getAllWorkspaces(@Param("organizationId") String organizationId);

    @RequestLine("GET /api/v1/organization/{organizationId}/workspace/{workspaceId}")
    Response<Workspace> getWorkspaceById(@Param("organizationId") String organizationId, @Param("workspaceId") String workspaceId);

    @RequestLine("GET /api/v1/organization/{organizationId}/workspace/{workspaceId}?include=variable")
    ResponseWithInclude<Workspace, Variable> getWorkspaceByIdWithVariables(@Param("organizationId") String organizationId, @Param("workspaceId") String workspaceId);

    @RequestLine("PATCH /api/v1/organization/{organizationId}/job/{jobId}")
    @Headers("Content-Type: application/vnd.api+json")
    void updateJob(JobRequest jobRequest, @Param("organizationId") String organizationId, @Param("jobId") String jobId);

    @RequestLine("GET /api/v1/organization/{organizationId}/job/{jobId}?include=step")
    ResponseWithInclude<Job, Step>getJobById(@Param("organizationId") String organizationId, @Param("jobId") String jobId);

    @RequestLine("POST /api/v1/organization/{organizationId}/job/{jobId}/step")
    @Headers("Content-Type: application/vnd.api+json")
    void createStep(StepRequest stepRequest, @Param("organizationId") String organizationId, @Param("jobId") String jobId);

    @RequestLine("POST /api/v1/organization/{organizationId}/workspace/{workspaceId}/history")
    @Headers("Content-Type: application/vnd.api+json")
    void createHistory(HistoryRequest historyRequest, @Param("organizationId") String organizationId, @Param("workspaceId") String workspaceId);

    @RequestLine("PATCH /api/v1/organization/{organizationId}/job/{jobId}/step/{stepId}")
    @Headers("Content-Type: application/vnd.api+json")
    void updateStep(StepRequest stepRequest, @Param("organizationId") String organizationId, @Param("jobId") String jobId, @Param("stepId") String stepId);

    @RequestLine("GET /api/v1/organization?filter[organization]=name=={organizationName}")
    Response<List<Organization>> getOrganizationByName(@Param("organizationName") String organizationName);

    @RequestLine("GET /api/v1/organization/{organizationId}/module?filter[module]=name=={moduleName};provider=={providerName}")
    Response<List<Module>> getModuleByNameAndProvider(@Param("organizationId") String organizationId, @Param("moduleName") String moduleName, @Param("providerName") String providerName);

    @RequestLine("GET /api/v1/organization/{organizationId}/module/{moduleId}")
    Response<Module> getModuleById(@Param("organizationId") String organizationId, @Param("moduleId") String moduleId);

    @RequestLine("PATCH /api/v1/organization/{organizationId}/module/{moduleId}")
    @Headers("Content-Type: application/vnd.api+json")
    void updateModule(ModuleRequest moduleRequest, @Param("organizationId") String organizationId, @Param("moduleId") String moduleId);
    
    @RequestLine("GET /api/v1/organization/{organizationId}/module/{moduleId}/version")
    Response<List<ModuleVersion>> getAllVersionsByOrganizationIdAndModuleId(@Param("organizationId") String organizationId, @Param("moduleId") String moduleId);

    @RequestLine("GET /api/v1/organization?filter[organization]=name=={organizationName}&filter[provider]=name=={providerName}")
    Response<List<Organization>> getOrganizationsByNameAndProvider(@Param("organizationName") String organizationName, @Param("providerName") String providerName);

    @RequestLine("GET /api/v1/organization/{organizationId}/provider/{providerId}/version?include=implementation")
    ResponseWithInclude<List<Version>, Implementation> getAllVersionsByProviderWithImplementation(@Param("organizationId") String organizationId, @Param("providerId") String providerId);

    @RequestLine("GET /api/v1/organization/{organizationId}/provider/{providerId}/version?filter[version]=versionNumber=={versionNumber}")
    Response<List<Version>> getVersionsByOrganizationIdAndProviderIdAndVersionNumber(@Param("organizationId") String organizationId, @Param("providerId") String providerId, @Param("versionNumber") String versionNumber);

    @RequestLine("GET /api/v1/organization/{organizationId}/provider/{providerId}/version/{versionId}/implementation?filter[implementation]=os=={os};arch=={arch}")
    Response<List<Implementation>> getImplementationByOsArchVersion(@Param("organizationId") String organizationId, @Param("providerId") String providerId, @Param("versionId") String versionId, @Param("os") String os, @Param("arch") String arch);

    @RequestLine("GET /api/v1/organization/{organizationId}/vcs/{vcsId}")
    Response<Vcs> getVcsById(@Param("organizationId") String organizationId, @Param("vcsId") String vcsId);
    
    @RequestLine("GET /api/v1/github_app_token?filter[github_app_token]=owner=={owner};appId=={appId}")
    Response<List<GitHubAppToken>> getGitHubAppTokenByVcsIdAndOwner( @Param("owner") String owner, @Param("appId") String appId);
    
    @RequestLine("GET /api/v1/organization/{organizationId}/ssh/{sshId}")
    Response<Ssh> getSshById(@Param("organizationId") String organizationId, @Param("sshId") String sshId);

    @RequestLine("GET /api/v1/organization/{organizationId}/template/{templateId}")
    Response<Template> getTemplateById(@Param("organizationId") String organizationId, @Param("templateId") String templateId);

    @RequestLine("POST /logs/{jobId}/setup-consumer-groups")
    @Headers("Content-Type: application/vnd.api+json")
    void setupConsumerGroups(@Param("jobId") String jobId);

    @RequestLine("POST /logs")
    @Headers("Content-Type: application/vnd.api+json")
    void appendLogs(LogsRequest logRequests);

    @RequestLine("POST /graphql/api/v1")
    @Headers("Content-Type: application/json")
    GraphQLResponse<SearchOrganizationResponse> searchOrganization(GraphQLRequest request);

    @RequestLine("POST /graphql/api/v1")
    @Headers("Content-Type: application/json")
    GraphQLResponse<SearchOrganizationModuleResponse> searchOrganizationModules(GraphQLRequest request);

    @RequestLine("POST /graphql/api/v1")
    @Headers("Content-Type: application/json")
    GraphQLResponse<SearchOrganizationProviderResponse> searchOrganizationProviders(GraphQLRequest request);
}
