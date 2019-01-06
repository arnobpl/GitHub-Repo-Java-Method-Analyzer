package restApi.github;

import restApi.RestApiClient;

/**
 * This class is responsible for handling REST APIs of Github API v3.
 */
public class GithubRestApiClient {
    //region constant variables

    private static final String GITHUB_API_ROOT_TARGET_URL = "https://api.github.com";

    //endregion


    //region variables

    private final RestApiClient restApiClient;

    private GithubRestApiRepoHelper repoHelper;

    //endregion


    //region constructors

    public GithubRestApiClient(String username, String password) {
        restApiClient = new RestApiClient(GITHUB_API_ROOT_TARGET_URL, username, password);
    }

    public GithubRestApiClient() {
        this("", "");
    }

    //endregion


    //region repoHelper

    public GithubRestApiRepoHelper getRepoHelper() {
        return repoHelper;
    }

    public void setRepoFullName(String repoFullName) {
        repoHelper = new GithubRestApiRepoHelper(repoFullName, restApiClient);
    }

    //endregion
}
