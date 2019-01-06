package restApi;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import retryUtil.RetryMethod;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.InvalidPropertiesFormatException;

/**
 * This class is responsible for handling REST APIs as a client.
 */
public class RestApiClient {
    //region constant variables

    private static final int DEFAULT_MAX_REQUEST_RETRY_TIME = 3;

    //endregion


    //region final variables

    private final String targetUrl;
    private final Client client;

    private final WebTarget rootWebTarget;

    //endregion


    //region variables

    private int maxRetryTime = DEFAULT_MAX_REQUEST_RETRY_TIME;

    //endregion


    //region constructors

    public RestApiClient(String targetUrl, String username, String password) {
        this.targetUrl = targetUrl;

        this.client = ClientBuilder.newBuilder().build();
        if (!username.isEmpty() && !password.isEmpty()) {
            client.register(HttpAuthenticationFeature.basic(username, password));
        }

        this.rootWebTarget = client.target(targetUrl);
    }

    public RestApiClient(String targetUrl) {
        this(targetUrl, "", "");
    }

    //endregion


    //region getter methods

    public String getTargetUrl() {
        return targetUrl;
    }

    public Client getClient() {
        return client;
    }

    public WebTarget getRootWebTarget() {
        return rootWebTarget;
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


    //region getResponse methods

    public JsonElement getJsonResponse(WebTarget webTarget) throws Exception {
        return new RetryMethod<JsonElement>(maxRetryTime) {
            @Override
            public JsonElement doTask() throws Exception {
                Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
                Response response = invocationBuilder.get();

                if (response.getStatus() != 200)
                    throw new InvalidPropertiesFormatException(getResponseExceptionMessage(response));

                String jsonResponse = response.readEntity(String.class);
                return new JsonParser().parse(jsonResponse);
            }
        }.doTaskWithRetry();
    }

    //endregion


    //region helper methods

    private static String getResponseExceptionMessage(Response response) {
        return Integer.toString(response.getStatus()) + ":\n" + response.readEntity(String.class);
    }

    //endregion
}
