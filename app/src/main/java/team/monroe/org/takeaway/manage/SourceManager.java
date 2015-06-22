package team.monroe.org.takeaway.manage;

import java.util.List;

public interface SourceManager {

    public Answer<String> getSourceVersion(SourceConfigurationManager.Configuration sourceConfiguration);
    public Answer<List<RemoteFile>> getTopFolder(SourceConfigurationManager.Configuration sourceConfiguration);
    public Answer<List<RemoteFile>> getFolderContent(SourceConfigurationManager.Configuration sourceConfiguration, String folderId);

    public static class RemoteFile{

        public final String remoteId;
        public final String title;
        public final boolean isFolder;

        public RemoteFile(String remoteId, String title, boolean isFolder) {
            this.remoteId = remoteId;
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
