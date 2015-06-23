package team.monroe.org.takeaway.manage;

import java.util.ArrayList;
import java.util.List;

import team.monroe.org.takeaway.manage.exceptions.FileOperationException;
import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.Source;

public class KodiFileProvider implements FileProvider {

    private final CloudManager cloudManager;
    private final CloudConfigurationManager cloudConfigurationManager;

    public KodiFileProvider(CloudManager cloudManager, CloudConfigurationManager cloudConfigurationManager) {
        this.cloudManager = cloudManager;
        this.cloudConfigurationManager = cloudConfigurationManager;
    }

    @Override
    public List<Source> getSources() throws FileOperationException {
        CloudConfigurationManager.Configuration configuration = cloudConfigurationManager.get();
        CloudManager.Answer<List<CloudManager.RemoteFile>> sources = cloudManager.getSources(configuration);
        if (!sources.isSuccess()){
            throw new FileOperationException(null, FileOperationException.ErrorCode.from(sources.status), sources.errorDescription);
        }
        List<Source> answer = new ArrayList<>();
        for (CloudManager.RemoteFile remoteFile : sources.body) {
            answer.add(new Source(remoteFile.title,remoteFile.title));
        }
        return answer;
    }

    @Override
    public List<FilePointer> getNestedFiles(FilePointer filePointer) throws FileOperationException {
        CloudConfigurationManager.Configuration configuration = cloudConfigurationManager.get();
        CloudManager.Answer<List<CloudManager.RemoteFile>> sources = cloudManager.getSources(configuration);

        if (!sources.isSuccess()){
            throw new FileOperationException(null, FileOperationException.ErrorCode.from(sources.status), sources.errorDescription);
        }

        CloudManager.RemoteFile sourceRemoteFile = null;
        for (CloudManager.RemoteFile file : sources.body) {
            if (filePointer.source.id.equals(file.title)){
                sourceRemoteFile = file;
                break;
            }
        }

        if (sourceRemoteFile == null){
            throw new FileOperationException(new NullPointerException(), FileOperationException.ErrorCode.FAILED, "No source with id = "+filePointer.source.id);
        }

        CloudManager.Answer<List<CloudManager.RemoteFile>> remoteSubFiles = cloudManager.getFolderContent(configuration, sourceRemoteFile.path + filePointer.relativePath);
        if (!remoteSubFiles.isSuccess()){
            throw new FileOperationException(null, FileOperationException.ErrorCode.from(sources.status), sources.errorDescription);
        }
        List<FilePointer> answer = new ArrayList<>();
        for (CloudManager.RemoteFile file : remoteSubFiles.body) {
            answer.add(new FilePointer(
                    filePointer.source,
                    truncate(file.path, sourceRemoteFile.path),
                    file.title,
                    file.isFolder? FilePointer.Type.FOLDER: FilePointer.Type.FILE));
        }
        return answer;
    }

    private String truncate(String path, String startWith) {
        return path.replaceFirst(startWith,"");
    }
}
