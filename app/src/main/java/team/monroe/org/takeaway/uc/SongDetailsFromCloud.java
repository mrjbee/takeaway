package team.monroe.org.takeaway.uc;

import org.monroe.team.corebox.log.L;
import org.monroe.team.corebox.services.ServiceRegistry;
import org.monroe.team.corebox.uc.UserCaseSupport;

import team.monroe.org.takeaway.manage.CloudConfigurationManager;
import team.monroe.org.takeaway.manage.CloudManager;
import team.monroe.org.takeaway.manage.CloudMetadataProvider;
import team.monroe.org.takeaway.manage.exceptions.FileOperationException;
import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.SongDetails;

public class SongDetailsFromCloud extends UserCaseSupport<FilePointer, SongDetails> {

    public SongDetailsFromCloud(ServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }

    @Override
    protected SongDetails executeImpl(FilePointer request) {
        try {
            return using(CloudMetadataProvider.class).getFiledDetails(request);
        } catch (FileOperationException e) {
            L.DEBUG.e("Cloud metadata error", e);
            return null;
        }
    }

}
