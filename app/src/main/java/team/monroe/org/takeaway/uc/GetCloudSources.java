package team.monroe.org.takeaway.uc;

import org.monroe.team.android.box.services.SettingManager;
import org.monroe.team.corebox.app.Model;
import org.monroe.team.corebox.services.ServiceRegistry;
import org.monroe.team.corebox.uc.UserCaseSupport;
import org.monroe.team.corebox.utils.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import team.monroe.org.takeaway.manage.CloudConfigurationManager;
import team.monroe.org.takeaway.manage.CloudManager;
import team.monroe.org.takeaway.manage.FileProvider;
import team.monroe.org.takeaway.manage.Settings;
import team.monroe.org.takeaway.manage.exceptions.ApplicationException;
import team.monroe.org.takeaway.manage.exceptions.FileOperationException;
import team.monroe.org.takeaway.presentations.Source;

public class GetCloudSources extends UserCaseSupport<Void, List<Source>>{

    public GetCloudSources(ServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }

    @Override
    protected List<Source> executeImpl(Void arg) {

        Boolean offlineMode = using(SettingManager.class).get(Settings.MODE_OFFLINE);
        if (Boolean.TRUE.equals(offlineMode)){
            //TODO: Implement offline fetching
            return Collections.emptyList();
        }
        List<Source> sourceList = null;
        try {
            sourceList = using(FileProvider.class).getSources();
        } catch (FileOperationException e) {
            throw new ApplicationException(e);
        }
        return sourceList;
    }
}
