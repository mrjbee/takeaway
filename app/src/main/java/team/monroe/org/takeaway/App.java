package team.monroe.org.takeaway;

import org.monroe.team.android.box.app.ApplicationSupport;
import org.monroe.team.android.box.data.Data;
import org.monroe.team.android.box.data.PersistRangeDataProvider;
import org.monroe.team.android.box.utils.AndroidLogImplementation;
import org.monroe.team.corebox.log.L;

import java.util.List;

import team.monroe.org.takeaway.manage.CloudConfigurationManager;
import team.monroe.org.takeaway.manage.CloudConnectionManager;
import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.Source;
import team.monroe.org.takeaway.presentations.SourceConnectionStatus;
import team.monroe.org.takeaway.uc.CheckCloudConnection;
import team.monroe.org.takeaway.uc.GetCloudSources;
import team.monroe.org.takeaway.uc.GetFileContent;

public class App extends ApplicationSupport<AppModel> {

    static {
        L.setup(new AndroidLogImplementation());
    }

    public PersistRangeDataProvider<FilePointer, List<FilePointer>> data_range_folder;
    public Data<List<Source>> data_sources;

    @Override
    protected void onPostCreate() {
        super.onPostCreate();
        data_sources = new Data<List<Source>>(model()) {
            @Override
            protected List<Source> provideData() {
                return model().execute(GetCloudSources.class, null);
            }
        };

        data_range_folder = new PersistRangeDataProvider<FilePointer, List<FilePointer>>() {
            @Override
            protected Data<List<FilePointer>> buildData(final FilePointer filePointer) {
                return new Data<List<FilePointer>>(model()) {
                    @Override
                    protected List<FilePointer> provideData() {
                        return model().execute(GetFileContent.class, filePointer);
                    }
                };
            }

            @Override
            protected String convertToStringKey(FilePointer filePointer) {
                return filePointer.source.id+":"+filePointer.relativePath;
            }
        };
        model().usingService(CloudConnectionManager.class).startWatcher();
    }

    @Override
    protected AppModel createModel() {
        return new AppModel("takeaway", getApplicationContext());
    }

    public boolean isSourceConfigured() {
        return model().usingService(CloudConfigurationManager.class).get() != null;
    }

    public CloudConfigurationManager.Configuration getSourceConfiguration() {
        return model().usingService(CloudConfigurationManager.class).get();
    }

    public void updateConfiguration(CloudConfigurationManager.Configuration configuration, ValueObserver<SourceConnectionStatus> observer) {
        fetchValue(CheckCloudConnection.class, configuration, new NoOpValueAdapter<SourceConnectionStatus>(), observer);
    }

    public CloudConnectionManager.ConnectionStatus getConnectionStatus() {
        return model().usingService(CloudConnectionManager.class).getStatus();
    }
}
