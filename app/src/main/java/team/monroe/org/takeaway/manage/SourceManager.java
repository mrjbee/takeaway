package team.monroe.org.takeaway.manage;

import java.util.List;

public interface SourceManager {

    public Answer<String> getSourceVersion(SourceConfigurationManager.Configuration sourceConfiguration);
    public Answer<List<RemoteFile>> getFolderContent(SourceConfigurationManager.Configuration sourceConfiguration, String folderPath);

    public static class RemoteFile{

        public final String title;
        public final String path;
        public final boolean isFolder;

        public RemoteFile(String title, String path, boolean isFolder) {
            this.title = title;
            this.path = path;
            this.isFolder = isFolder;
        }
    }

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
            return status == Status.SUCCESS;
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
