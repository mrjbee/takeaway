package team.monroe.org.takeaway.manage.exceptions;

public class ApplicationException extends RuntimeException {
    public ApplicationException(Throwable throwable) {
        super(throwable);
    }

    public static <ExceptionType extends Exception> ExceptionType get(Throwable throwable, Class<ExceptionType> asExceptionClass) {
        if (asExceptionClass.isInstance(throwable)){
            return (ExceptionType) throwable;
        }else if (asExceptionClass.isInstance(throwable.getCause())){
            return (ExceptionType) throwable.getCause();
        }
        return null;
    }
}
