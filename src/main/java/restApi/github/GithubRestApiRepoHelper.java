package restApi.github;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import restApi.RestApiClient;

import javax.ws.rs.client.WebTarget;

/**
 * This class is responsible for handling REST APIs of Github API v3 for a repo.
 */
public class GithubRestApiRepoHelper {
    //region constant variables

    private static final String GITHUB_ROOT_REPOS_TARGET_URL = "repos";
    private static final String GITHUB_REPO_COMMITS_TARGET_URL = "commits";

    private static final String GITHUB_COMMIT_MASTER = "master";
    private static final String GITHUB_COMMIT_SHA = "sha";
    private static final String GITHUB_COMMIT_PARENTS = "parents";
    private static final String GITHUB_COMMIT_FILES = "files";
    private static final String GITHUB_PAGE_PARAM = "page";

    //endregion


    //region variables

    private final String repoFullName;

    private final RestApiClient restApiClient;

    private final WebTarget repoWebTarget;
    private final WebTarget repoCommitsWebTarget;

    private final GithubRestApiRepoFileHelper repoFileHelper;

    //endregion


    //region constructors

    public GithubRestApiRepoHelper(String repoFullName, RestApiClient restApiClient) {
        this.repoFullName = repoFullName;
        this.restApiClient = restApiClient;

        this.repoWebTarget = restApiClient.getRootWebTarget().path(GITHUB_ROOT_REPOS_TARGET_URL).path(repoFullName);
        this.repoCommitsWebTarget = repoWebTarget.path(GITHUB_REPO_COMMITS_TARGET_URL);

        this.repoFileHelper = new GithubRestApiRepoFileHelper(repoFullName);
    }

    //endregion


    //region repoFullName getter

    public String getRepoFullName() {
        return repoFullName;
    }

    //endregion


    //region repoFileHelper getter

    public GithubRestApiRepoFileHelper getRepoFileHelper() {
        return repoFileHelper;
    }

    //endregion


    //region methods for getting response

    public JsonObject getRepoInfo() throws Exception {
        return restApiClient.getJsonResponse(repoWebTarget).getAsJsonObject();
    }

    public JsonArray getRepoCommits(int page) throws Exception {
        return restApiClient.getJsonResponse(repoCommitsWebTarget.queryParam(GITHUB_PAGE_PARAM, Integer.toString(page))).getAsJsonArray();
    }

    public JsonArray getRepoCommits() throws Exception {
        return getRepoCommits(1);
    }

    public JsonObject getCommitInfo(String commitSha) throws Exception {
        return restApiClient.getJsonResponse(repoCommitsWebTarget.path(commitSha)).getAsJsonObject();
    }

    public JsonObject getLatestCommitInfo(String branchName) throws Exception {
        return restApiClient.getJsonResponse(repoCommitsWebTarget.path(branchName)).getAsJsonObject();
    }

    public JsonObject getLatestCommitInfo() throws Exception {
        return getLatestCommitInfo(GITHUB_COMMIT_MASTER);
    }

    public static String getCommitSha(JsonObject commitInfo) {
        return commitInfo.get(GITHUB_COMMIT_SHA).getAsString();
    }

    public static String getPreviousCommitSha(JsonObject currentCommitInfo) {
        JsonArray parents = currentCommitInfo.get(GITHUB_COMMIT_PARENTS).getAsJsonArray();
        if (parents.size() == 0) return null;

        return getCommitSha(parents.get(0).getAsJsonObject());
    }

    public static JsonArray getCommitFiles(JsonObject commitInfo) {
        return commitInfo.get(GITHUB_COMMIT_FILES).getAsJsonArray();
    }

    //endregion
}
