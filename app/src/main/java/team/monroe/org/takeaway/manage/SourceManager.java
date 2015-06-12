package team.monroe.org.takeaway.manage;

public interface SourceManager {

    public Answer<String> getSourceVersion(SourceConfigurationManager.Configuration sourceConfiguration);

    public static class Answer<ResponseBody>{

        public final Status status;
        public final String errorDescription;
        public final ResponseBody body;

        public Answer(Status status, String errorDescription, ResponseBody body) {
            this.status = status;
            this.errorDescription = errorDescription;
            this.body = body;
        }

        public boolean isSuccess(){
            return status != Status.SUCCESS;
        }

    }

    public static enum Status {
        SUCCESS,
        FAILED,
        NO_ROUTE_TO_HOST,
        BAD_URL,
        BAD_CONNECTION,
        INVALID_RESPONSE,
        UNSUPPORTED_FORMAT
    }
}
