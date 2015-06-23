package team.monroe.org.takeaway.manage.exceptions;

public class ApplicationException extends RuntimeException {
    public ApplicationException(Throwable throwable) {
        super(throwable);
    }
}
