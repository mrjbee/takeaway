package team.monroe.org.takeaway.uc;

import org.monroe.team.corebox.services.ServiceRegistry;
import org.monroe.team.corebox.uc.UserCaseSupport;

import java.util.ArrayList;
import java.util.List;

import team.monroe.org.takeaway.manage.CloudConfigurationManager;
import team.monroe.org.takeaway.manage.CloudManager;
import team.monroe.org.takeaway.manage.FileProvider;
import team.monroe.org.takeaway.manage.exceptions.ApplicationException;
import team.monroe.org.takeaway.manage.exceptions.FileOperationException;
import team.monroe.org.takeaway.presentations.Source;

public class GetCloudSources extends UserCaseSupport<Void, List<Source>>{

    public GetCloudSources(ServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }

    @Override
    protected List<Source> executeImpl(Void request) {
        List<Source> sourceList = null;
        try {
            sourceList = using(FileProvider.class).getSources();
        } catch (FileOperationException e) {
            throw new ApplicationException(e);
        }
        return sourceList;
    }
}
