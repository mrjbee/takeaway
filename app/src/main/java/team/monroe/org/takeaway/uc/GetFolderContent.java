package team.monroe.org.takeaway.uc;

import org.monroe.team.corebox.services.ServiceRegistry;
import org.monroe.team.corebox.uc.UserCaseSupport;

import java.util.ArrayList;
import java.util.List;

import team.monroe.org.takeaway.manage.CloudConfigurationManager;
import team.monroe.org.takeaway.manage.CloudManager;
import team.monroe.org.takeaway.presentations.Folder;
import team.monroe.org.takeaway.presentations.FolderContent;
import team.monroe.org.takeaway.presentations.SourceConnectionStatus;

public class GetFolderContent extends UserCaseSupport<Folder, FolderContent>{

    public GetFolderContent(ServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }

    @Override
    protected FolderContent executeImpl(Folder request) {

        CloudConfigurationManager.Configuration configuration = using(CloudConfigurationManager.class).get();

        CloudManager.Answer<List<CloudManager.RemoteFile>> folderAnswer = null;

        if (request == Folder.FOLDER_ROOT){
            folderAnswer = using(CloudManager.class).getSources(configuration);
        } else {
            folderAnswer = using(CloudManager.class).getFolderContent(configuration, request.id);
        }

        SourceConnectionStatus connectionStatus = SourceConnectionStatus.fromAnswer(folderAnswer);

        List<Folder> folders = new ArrayList<>();
        if (folderAnswer.isSuccess()){
            for (CloudManager.RemoteFile remoteFile : folderAnswer.body) {
                    folders.add(new Folder(remoteFile.path, remoteFile.title));

            }
        }
        return new FolderContent(request, folders);
    }
}
