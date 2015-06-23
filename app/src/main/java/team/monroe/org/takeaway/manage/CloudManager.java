package team.monroe.org.takeaway.manage;

import java.util.List;

public interface CloudManager {

    public Answer<String> getSourceVersion(CloudConfigurationManager.Configuration sourceConfiguration);
    public Answer<List<RemoteFile>> getSources(CloudConfigurationManager.Configuration sourceConfiguration);
    public Answer<List<RemoteFile>> getFolderContent(CloudConfigurationManager.Configuration sourceConfiguration, String folderId);

    public static class RemoteFile {

        public final String path;
        public final String title;
        public final boolean isFolder;

        public RemoteFile(String path, String title, boolean isFolder) {
            this.path = path;
            this.title = title;
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
