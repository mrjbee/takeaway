package team.monroe.org.takeaway.uc;

import org.monroe.team.corebox.services.ServiceRegistry;
import org.monroe.team.corebox.uc.UserCaseSupport;

import java.util.Collections;

import team.monroe.org.takeaway.presentations.Folder;
import team.monroe.org.takeaway.presentations.FolderContent;

public class GetFolderContent extends UserCaseSupport<Folder, FolderContent>{

    public GetFolderContent(ServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }

    @Override
    protected FolderContent executeImpl(Folder request) {
        return new FolderContent(request, Collections.EMPTY_LIST);
    }
}
