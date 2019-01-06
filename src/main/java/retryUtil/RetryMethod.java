package retryUtil;

/**
 * This class helps calling a specific method with multiple retry time.
 *
 * @param <T> return type of the intended method
 */
public abstract class RetryMethod<T> {
    //region constant variables

    private static final String DEFAULT_RETRY_OUTPUT_STRING = "Retrying...";

    //endregion


    //region variables

    private String retryOutputString = DEFAULT_RETRY_OUTPUT_STRING;
    private int maxRetryTime;

    //endregion


    //region constructors

    public RetryMethod(int maxRetryTime) {
        this.maxRetryTime = maxRetryTime;
    }

    //endregion


    //region getter and setter methods

    public String getRetryOutputString() {
        return retryOutputString;
    }

    public void setRetryOutputString(String retryOutputString) {
        this.retryOutputString = retryOutputString;
    }

    public int getMaxRetryTime() {
        return maxRetryTime;
    }

    public void setMaxRetryTime(int maxRetryTime) {
        this.maxRetryTime = maxRetryTime;
    }

    //endregion


    //region abstract methods

    public abstract T doTask() throws Exception;

    //endregion


    //region methods

    public T doTaskWithRetry() throws Exception {
        int retry = 0;
        Exception exception;
        do {
            if (retry > 0)
                System.out.println(retryOutputString);

            try {
                return doTask();
            } catch (Exception e) {
                exception = e;
                exception.printStackTrace();
                retry++;
            }
        } while (retry < maxRetryTime);

        throw exception;
    }

    //endregion
}
