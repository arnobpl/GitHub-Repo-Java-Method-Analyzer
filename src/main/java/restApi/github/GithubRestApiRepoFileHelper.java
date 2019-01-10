package restApi.github;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jsonUtil.JsonArrayFilterer;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.util.EntityUtils;
import retryUtil.RetryMethod;

import java.io.IOException;
import java.util.InvalidPropertiesFormatException;

/**
 * This class is responsible for handling REST APIs of Github API v3 for committed files.
 */
public class GithubRestApiRepoFileHelper {
    //region constant variables

    private static final String GITHUB_ROOT_RAW_FILE_TARGET_URL = "https://github.com";
    private static final String GITHUB_RAW_FILE_TARGET_URL = "raw";

    private static final String GITHUB_COMMIT_FILE_NAME = "filename";
    private static final String GITHUB_COMMIT_FILE_STATUS = "status";
    private static final String GITHUB_COMMIT_FILE_RAW_URL = "raw_url";

    private static final int DEFAULT_MAX_REQUEST_RETRY_TIME = 5;

    //endregion


    //region enums

    public enum FileStatus {ADDED, REMOVED, MODIFIED}

    //endregion


    //region variables

    private final String repoRawFileTargetUrl;

    private int maxRetryTime = DEFAULT_MAX_REQUEST_RETRY_TIME;

    //endregion


    //region constructors

    public GithubRestApiRepoFileHelper(String repoFullName) {
        this.repoRawFileTargetUrl = (GITHUB_ROOT_RAW_FILE_TARGET_URL + "/" + repoFullName + "/" + GITHUB_RAW_FILE_TARGET_URL);
    }

    //endregion


    //region maxRetryTime getter and setter

    public int getMaxRetryTime() {
        return maxRetryTime;
    }

    public void setMaxRetryTime(int maxRetryTime) {
        this.maxRetryTime = maxRetryTime;
    }

    //endregion


    //region methods for extracting file properties

    public static String getFileName(JsonObject fileJsonObject) {
        return fileJsonObject.get(GITHUB_COMMIT_FILE_NAME).getAsString();
    }

    public static String getFileStatus(JsonObject fileJsonObject) {
        return fileJsonObject.get(GITHUB_COMMIT_FILE_STATUS).getAsString();
    }

    public static String getFileRawUrl(JsonObject fileJsonObject) {
        return fileJsonObject.get(GITHUB_COMMIT_FILE_RAW_URL).getAsString();
    }

    //endregion


    //region methods for filtering files

    public static JsonArray getFilesByExtension(JsonArray fileJsonArray, String fileExtension) {
        return new JsonArrayFilterer(fileJsonArray) {
            @Override
            public boolean shouldBeIncluded(JsonElement jsonElement) {
                return (jsonElement.getAsJsonObject().get(GITHUB_COMMIT_FILE_NAME).getAsString().endsWith(fileExtension));
            }
        }.filterJsonArray();
    }

    public static JsonArray getFilesByStatus(JsonArray fileJsonArray, FileStatus fileStatus) {
        return new JsonArrayFilterer(fileJsonArray) {
            private final String fileStatusString = fileStatus.name().toLowerCase();

            @Override
            public boolean shouldBeIncluded(JsonElement jsonElement) {
                return (jsonElement.getAsJsonObject().get(GITHUB_COMMIT_FILE_STATUS).getAsString().equals(fileStatusString));
            }
        }.filterJsonArray();
    }

    //endregion


    //region methods for getting file

    /**
     * This method downloads a specific file of a given commit as text.
     *
     * @param commitSha commit SHA value of a commit (version of a file)
     * @param fileName  file name (Github file path)
     * @return String representation of a specific file of a given commit as text
     * @throws Exception if any error occurs (such as "file not found")
     */
    public String getRepoFileAsString(String commitSha, String fileName) throws Exception {
        return new RetryMethod<String>(maxRetryTime) {
            @Override
            public String doTask() throws Exception {
                HttpResponse response = Request.Get(repoRawFileTargetUrl + "/" + commitSha + "/" + fileName).execute().returnResponse();

                if (response.getStatusLine().getStatusCode() != 200)
                    throw new InvalidPropertiesFormatException(getResponseExceptionMessage(response));

                return EntityUtils.toString(response.getEntity());
            }
        }.doTaskWithRetry();
    }

    //endregion


    //region helper methods

    private static String getResponseExceptionMessage(HttpResponse response) throws IOException {
        return Integer.toString(response.getStatusLine().getStatusCode()) + ":\n" +
                EntityUtils.toString(response.getEntity());
    }

    //endregion
}
