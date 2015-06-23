package team.monroe.org.takeaway.presentations;

import android.content.res.Resources;

import team.monroe.org.takeaway.manage.CloudManager;

public class SourceConnectionStatus {

    public final CloudManager.Status status;
    public final String errorDetails;

    public SourceConnectionStatus(CloudManager.Status status, String errorDetails) {
        this.status = status;
        this.errorDetails = errorDetails;
    }

    public static SourceConnectionStatus fromAnswer(CloudManager.Answer<?> sourceAnswer) {
        return new SourceConnectionStatus(sourceAnswer.status, sourceAnswer.errorDescription);
    }

    public boolean isSuccess() {
        return status == CloudManager.Status.SUCCESS;
    }

    public String asString(Resources resources) {
        switch (status){
            case BAD_CONNECTION:
                return wrapWithDetails("Source not accessible");
            case BAD_URL:
                return wrapWithDetails("Invalid URL");
            case FAILED:
                return wrapWithDetails("Source communication failed");
            case INVALID_RESPONSE:
                return wrapWithDetails("Invalid Source answer");
            case NO_ROUTE_TO_HOST:
                return wrapWithDetails("No access to Source host");
            case UNSUPPORTED_FORMAT:
                return wrapWithDetails("Unsupported Source answer");
            case SUCCESS:
                return wrapWithDetails("Success");
            default:
                throw new IllegalStateException();
        }
    }

    private String wrapWithDetails(String msg) {
        if (errorDetails != null && !errorDetails.isEmpty()){
            return msg +" ("+errorDetails+")";
        }
        return msg;
    }
}
