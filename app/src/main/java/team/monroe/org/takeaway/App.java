package team.monroe.org.takeaway;

import android.app.Activity;

import org.monroe.team.android.box.app.ApplicationSupport;
import org.monroe.team.android.box.data.Data;
import org.monroe.team.android.box.data.PersistRangeDataProvider;
import org.monroe.team.android.box.utils.AndroidLogImplementation;
import org.monroe.team.corebox.log.L;

import team.monroe.org.takeaway.manage.SourceConfigurationManager;
import team.monroe.org.takeaway.presentations.Folder;
import team.monroe.org.takeaway.presentations.FolderContent;
import team.monroe.org.takeaway.presentations.SourceConnectionStatus;
import team.monroe.org.takeaway.uc.CheckSourceConnection;
import team.monroe.org.takeaway.uc.GetFolderContent;

public class App extends ApplicationSupport<AppModel> {

    static {
        L.setup(new AndroidLogImplementation());
    }

    public PersistRangeDataProvider<Folder, FolderContent> data_range_folder;

    @Override
    protected void onPostCreate() {
        super.onPostCreate();

        data_range_folder = new PersistRangeDataProvider<Folder, FolderContent>() {

            @Override
            protected Data<FolderContent> buildData(final Folder folder) {
                return new Data<FolderContent>(model()) {
                    @Override
                    protected FolderContent provideData() {
                        return model().execute(GetFolderContent.class, folder);
                    }
                };
            }

            @Override
            protected String convertToStringKey(Folder folder) {
                return folder.path;
            }
        };
    }

    @Override
    protected AppModel createModel() {
        return new AppModel("takeaway", getApplicationContext());
    }

    public boolean isSourceConfigured() {
        return model().usingService(SourceConfigurationManager.class).get() != null;
    }

    public SourceConfigurationManager.Configuration getSourceConfiguration() {
        return model().usingService(SourceConfigurationManager.class).get();
    }

    public void updateConfiguration(SourceConfigurationManager.Configuration configuration, ValueObserver<SourceConnectionStatus> observer) {
        fetchValue(CheckSourceConnection.class, configuration, new NoOpValueAdapter<SourceConnectionStatus>(), observer);
    }
}
