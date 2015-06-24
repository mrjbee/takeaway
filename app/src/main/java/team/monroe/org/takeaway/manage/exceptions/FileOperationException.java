package team.monroe.org.takeaway.manage.exceptions;

import team.monroe.org.takeaway.manage.CloudManager;

public class FileOperationException extends Exception{

    public final ErrorCode errorCode;
    public final String extraDescription;

    public FileOperationException(Throwable throwable, ErrorCode errorCode, String extraDescription) {
        super(errorCode.name()+" :"+extraDescription,throwable);
        this.errorCode = errorCode;
        this.extraDescription = extraDescription;
    }

    public static enum ErrorCode{

        FAILED,
        NO_ROUTE_TO_HOST,
        BAD_URL,
        BAD_CONNECTION,
        INVALID_RESPONSE,
        UNSUPPORTED_FORMAT;

        public static ErrorCode from(CloudManager.Status status) {
            switch (status){
                case BAD_CONNECTION:
                    return BAD_CONNECTION;
                case NO_ROUTE_TO_HOST:
                    return NO_ROUTE_TO_HOST;
                case BAD_URL:
                    return BAD_URL;
                case INVALID_RESPONSE:
                    return INVALID_RESPONSE;
                case UNSUPPORTED_FORMAT:
                    return UNSUPPORTED_FORMAT;
                default:
                    return FAILED;
            }
        }
    }

}
