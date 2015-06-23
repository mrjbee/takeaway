package team.monroe.org.takeaway.uc;

import org.monroe.team.corebox.services.ServiceRegistry;
import org.monroe.team.corebox.uc.UserCaseSupport;

import java.util.List;

import team.monroe.org.takeaway.manage.FileProvider;
import team.monroe.org.takeaway.manage.exceptions.ApplicationException;
import team.monroe.org.takeaway.manage.exceptions.FileOperationException;
import team.monroe.org.takeaway.presentations.FilePointer;

public class GetFileContent extends UserCaseSupport<FilePointer, List<FilePointer>>{

    public GetFileContent(ServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }

    @Override
    protected List<FilePointer> executeImpl(FilePointer request) {
        FileProvider fileProvider = using(FileProvider.class);
        try {
            return fileProvider.getNestedFiles(request);
        } catch (FileOperationException e) {
            throw new ApplicationException(e);
        }
    }

}
