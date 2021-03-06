package team.monroe.org.takeaway.manage.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import team.monroe.org.takeaway.manage.CloudConfigurationManager;
import team.monroe.org.takeaway.manage.CloudConnectionManager;
import team.monroe.org.takeaway.manage.CloudManager;
import team.monroe.org.takeaway.manage.CloudMetadataProvider;
import team.monroe.org.takeaway.manage.StorageProvider;
import team.monroe.org.takeaway.manage.exceptions.FileOperationException;
import team.monroe.org.takeaway.presentations.AwarePath;
import team.monroe.org.takeaway.presentations.AwareSource;
import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.SongDetails;
import team.monroe.org.takeaway.presentations.Source;
import team.monroe.org.takeaway.presentations.SourceConnectionStatus;

public class KodiCloudProvider implements StorageProvider, CloudMetadataProvider {

    private final CloudManager cloudManager;
    private final CloudConnectionManager cloudConnectionManager;
    private final CloudConfigurationManager cloudConfigurationManager;

    public KodiCloudProvider(CloudManager cloudManager, CloudConnectionManager cloudConnectionManager, CloudConfigurationManager cloudConfigurationManager) {
        this.cloudManager = cloudManager;
        this.cloudConnectionManager = cloudConnectionManager;
        this.cloudConfigurationManager = cloudConfigurationManager;
    }

    @Override
    public List<Source> sources() throws FileOperationException {
        CloudConfigurationManager.Configuration configuration = cloudConfigurationManager.get();
        CloudManager.Answer<List<CloudManager.RemoteFile>> sources = cloudManager.getSources(configuration);
        cloudConnectionManager.updateStatusBySourceConnection(SourceConnectionStatus.fromAnswer(sources));
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
    public String absolutePath(FilePointer filePointer) throws FileOperationException {
        CloudConfigurationManager.Configuration configuration = cloudConfigurationManager.get();
        CloudManager.RemoteFile sourceRemoteFile = getSourceForFilePointer(filePointer, configuration);
        return sourceRemoteFile.path + filePointer.relativePath;
    }

    @Override
    public SongDetails getFiledDetails(FilePointer filePointer) throws FileOperationException {
        if (filePointer.type != FilePointer.Type.FILE) return null;
        CloudConfigurationManager.Configuration configuration = cloudConfigurationManager.get();
        CloudManager.RemoteFile sourceRemoteFile = getSourceForFilePointer(filePointer, configuration);
        CloudManager.Answer<Map<String,String>> fileDetails = cloudManager.getFileDetailsMap(configuration, sourceRemoteFile.path + filePointer.relativePath);
        if (!fileDetails.isSuccess()){
            throw new FileOperationException(null, FileOperationException.ErrorCode.from(fileDetails.status), fileDetails.errorDescription);
        }

        String artist = fileDetails.body.get("artist");
        String album = fileDetails.body.get("album");
        String title = fileDetails.body.get("title");
        if (artist != null){
            return new SongDetails(artist, album, title);
        }
        return null;
    }

    @Override
    public List<FilePointer> list(AwarePath awarePath) throws FileOperationException {
        CloudConfigurationManager.Configuration configuration = cloudConfigurationManager.get();
        CloudManager.RemoteFile sourceRemoteFile = getSourceForFilePointer(awarePath, configuration);

        CloudManager.Answer<List<CloudManager.RemoteFile>> remoteSubFiles = cloudManager.getFolderContent(configuration, sourceRemoteFile.path + awarePath.getRelativePath());
        if (!remoteSubFiles.isSuccess()){
            throw new FileOperationException(null, FileOperationException.ErrorCode.from(remoteSubFiles.status), remoteSubFiles.errorDescription);
        }
        List<FilePointer> answer = new ArrayList<>();
        for (CloudManager.RemoteFile file : remoteSubFiles.body) {
            answer.add(new FilePointer(
                    awarePath.getSource(),
                    truncate(file.path, sourceRemoteFile.path),
                    file.isFolder? FilePointer.Type.FOLDER: FilePointer.Type.FILE));
        }
        return answer;
    }

    private CloudManager.RemoteFile getSourceForFilePointer(AwareSource sourceAware, CloudConfigurationManager.Configuration configuration) throws FileOperationException {
        CloudManager.Answer<List<CloudManager.RemoteFile>> files = cloudManager.getSources(configuration);
        cloudConnectionManager.updateStatusBySourceConnection(SourceConnectionStatus.fromAnswer(files));
        if (!files.isSuccess()){
            throw new FileOperationException(null, FileOperationException.ErrorCode.from(files.status), files.errorDescription);
        }

        CloudManager.RemoteFile sourceRemoteFile = null;
        for (CloudManager.RemoteFile file : files.body) {
            if (sourceAware.getSource().id.equals(file.title)){
                sourceRemoteFile = file;
                break;
            }
        }

        if (sourceRemoteFile == null){
            throw new FileOperationException(new NullPointerException(), FileOperationException.ErrorCode.FAILED, "No source with id = "+sourceAware.getSource().id);
        }

        return sourceRemoteFile;
    }

    private String truncate(String path, String startWith) {
        return path.replaceFirst(startWith,"");
    }

}
