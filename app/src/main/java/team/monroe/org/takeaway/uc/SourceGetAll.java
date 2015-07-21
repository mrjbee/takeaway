package team.monroe.org.takeaway.uc;

import org.monroe.team.android.box.services.SettingManager;
import org.monroe.team.corebox.services.ServiceRegistry;
import org.monroe.team.corebox.uc.UserCaseSupport;
import org.monroe.team.corebox.utils.Closure;
import org.monroe.team.corebox.utils.Lists;

import java.util.ArrayList;
import java.util.List;

import team.monroe.org.takeaway.manage.StorageProvider;
import team.monroe.org.takeaway.manage.Settings;
import team.monroe.org.takeaway.manage.exceptions.ApplicationException;
import team.monroe.org.takeaway.manage.exceptions.FileOperationException;
import team.monroe.org.takeaway.presentations.Source;

public class SourceGetAll extends UserCaseSupport<Void, List<Source>>{

    public SourceGetAll(ServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }

    @Override
    protected List<Source> executeImpl(Void arg) {
        List<Source> localSourceList = new ArrayList<>();
        //TODO: implement local sources
        Boolean offlineMode = using(SettingManager.class).get(Settings.MODE_OFFLINE);
        if (Boolean.TRUE.equals(offlineMode)){
            //TODO: Implement offline fetching
            return localSourceList;
        }
        List<Source> sourceList = null;
        try {
            sourceList = using(StorageProvider.class).sources();
        } catch (FileOperationException e) {
            throw new ApplicationException(e);
        }

        for (final Source source : sourceList) {
           if (Lists.find(localSourceList, new Closure<Source, Boolean>() {
               @Override
               public Boolean execute(Source arg) {
                   return arg.id.equals(source.id);
               }
           }) == null){
               localSourceList.add(source);
           }
        }
        return localSourceList;
    }
}
