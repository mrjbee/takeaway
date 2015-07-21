package team.monroe.org.takeaway.uc;

import org.monroe.team.android.box.services.SettingManager;
import org.monroe.team.corebox.services.ServiceRegistry;
import org.monroe.team.corebox.uc.UserCaseSupport;

import team.monroe.org.takeaway.manage.CloudConfigurationManager;
import team.monroe.org.takeaway.manage.CloudManager;
import team.monroe.org.takeaway.manage.DownloadManager;
import team.monroe.org.takeaway.manage.StorageProvider;
import team.monroe.org.takeaway.manage.Settings;
import team.monroe.org.takeaway.manage.exceptions.ApplicationException;
import team.monroe.org.takeaway.manage.exceptions.FileOperationException;
import team.monroe.org.takeaway.presentations.FilePointer;

public class TransferGetForFile extends UserCaseSupport<FilePointer, DownloadManager.Transfer> {

    public TransferGetForFile(ServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }

    @Override
    protected DownloadManager.Transfer executeImpl(FilePointer request) {
        Boolean offlineMode = using(SettingManager.class).get(Settings.MODE_OFFLINE);
        if (Boolean.TRUE.equals(offlineMode)){
            //TODO: Implement offline fetching
            throw new OfflineModeEnabledException();
        }

        CloudManager cloudManager = using(CloudManager.class);
        StorageProvider storageProvider = using(StorageProvider.class);

        CloudManager.Answer<DownloadManager.Transfer> answer = null;
        try {
            answer = cloudManager.createTransfer(using(CloudConfigurationManager.class).get(), storageProvider.absolutePath(request));
        } catch (FileOperationException e) {
            throw new FileApplicationException(e);
        }
        if (!answer.isSuccess()){
            throw new FileApplicationException(new FileOperationException(null, FileOperationException.ErrorCode.from(answer.status), answer.errorDescription));
        }
        return answer.body;
    }

    public static class OfflineModeEnabledException extends ApplicationException{
        public OfflineModeEnabledException() {
            super(null);
        }
    }

    public static class FileApplicationException extends ApplicationException{
        public FileApplicationException (FileOperationException cause) {
            super(cause);
        }
    }
}
