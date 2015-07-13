package team.monroe.org.takeaway.uc;

import org.monroe.team.android.box.services.SettingManager;
import org.monroe.team.corebox.app.Model;
import org.monroe.team.corebox.services.ServiceRegistry;
import org.monroe.team.corebox.uc.UserCaseSupport;
import org.monroe.team.corebox.utils.Closure;
import org.monroe.team.corebox.utils.Lists;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import team.monroe.org.takeaway.manage.CloudManager;
import team.monroe.org.takeaway.manage.FileProvider;
import team.monroe.org.takeaway.manage.Settings;
import team.monroe.org.takeaway.manage.exceptions.ApplicationException;
import team.monroe.org.takeaway.manage.exceptions.FileOperationException;
import team.monroe.org.takeaway.presentations.FilePointer;

public class GetFileContent extends UserCaseSupport<FilePointer, List<FilePointer>>{

    public GetFileContent(ServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }

    @Override
    protected List<FilePointer> executeImpl(FilePointer request) {

        Boolean offlineMode = using(SettingManager.class).get(Settings.MODE_OFFLINE);
        if (Boolean.TRUE.equals(offlineMode)){
            //TODO: Implement offline fetching
            return Collections.emptyList();
        }

        final FileProvider fileProvider = using(FileProvider.class);
        final CloudManager cloudManager = using(CloudManager.class);

        try {
            final List<FilePointer> answer = fileProvider.getNestedFiles(request);
            Lists.each(answer, new Closure<FilePointer, Void>() {
                @Override
                public Void execute(FilePointer arg) {
                    arg.details = using(Model.class).execute(SongDetailsFromDB.class, arg.getSongId());
                    if (arg.details == null){
                        arg.details = using(Model.class).execute(SongDetailsFromCloud.class, arg);
                        if (arg.details != null){
                            using(Model.class).execute(SongDetailsSave.class, arg);
                        }
                    }
                    return null;
                }
            });
            Collections.sort(answer, new Comparator<FilePointer>() {
                @Override
                public int compare(FilePointer lhs, FilePointer rhs) {
                    return lhs.name.compareTo(rhs.name);
                }
            });

            return answer;
        } catch (FileOperationException e) {
            throw new ApplicationException(e);
        }
    }

}
