package team.monroe.org.takeaway.uc;

import org.monroe.team.corebox.services.ServiceRegistry;
import org.monroe.team.corebox.uc.UserCaseSupport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import team.monroe.org.takeaway.manage.SourceConfigurationManager;
import team.monroe.org.takeaway.manage.SourceManager;
import team.monroe.org.takeaway.presentations.Folder;
import team.monroe.org.takeaway.presentations.FolderContent;
import team.monroe.org.takeaway.presentations.SourceConnectionStatus;

public class GetFolderContent extends UserCaseSupport<Folder, FolderContent>{

    public GetFolderContent(ServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }

    @Override
    protected FolderContent executeImpl(Folder request) {

        SourceConfigurationManager.Configuration configuration = using(SourceConfigurationManager.class).get();
        SourceManager.Answer<List<SourceManager.RemoteFile>> folderAnswer = using(SourceManager.class).getFolderContent(configuration, "");

        SourceConnectionStatus connectionStatus = SourceConnectionStatus.fromAnswer(folderAnswer);

        List<Folder> folders = new ArrayList<>();
        if (folderAnswer.isSuccess()){
            for (SourceManager.RemoteFile remoteFile : folderAnswer.body) {
                folders.add(new Folder(remoteFile.path));
            }

        }
        return new FolderContent(request, folders);
    }
}
