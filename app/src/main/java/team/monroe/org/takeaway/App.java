package team.monroe.org.takeaway;

import org.monroe.team.android.box.app.ApplicationSupport;
import org.monroe.team.android.box.data.Data;
import org.monroe.team.android.box.data.PersistRangeDataProvider;
import org.monroe.team.android.box.event.Event;
import org.monroe.team.android.box.services.SettingManager;
import org.monroe.team.android.box.utils.AndroidLogImplementation;
import org.monroe.team.corebox.log.L;
import org.monroe.team.corebox.utils.Closure;
import org.monroe.team.corebox.utils.ObserverSupport;

import java.util.List;

import team.monroe.org.takeaway.manage.CloudConfigurationManager;
import team.monroe.org.takeaway.manage.CloudConnectionManager;
import team.monroe.org.takeaway.manage.Events;
import team.monroe.org.takeaway.manage.Player;
import team.monroe.org.takeaway.manage.Settings;
import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.SongDetails;
import team.monroe.org.takeaway.presentations.SongFile;
import team.monroe.org.takeaway.presentations.Source;
import team.monroe.org.takeaway.presentations.SourceConnectionStatus;
import team.monroe.org.takeaway.uc.CheckCloudConnection;
import team.monroe.org.takeaway.uc.SongDetailsExtractIfNeeded;
import team.monroe.org.takeaway.uc.GetCloudSources;
import team.monroe.org.takeaway.uc.GetFileContent;

public class App extends ApplicationSupport<AppModel> implements AppModel.DownloadObserver{

    static {
        L.setup(new AndroidLogImplementation());
    }

    public PersistRangeDataProvider<FilePointer, List<FilePointer>> data_range_folder;
    public Data<List<Source>> data_sources;
    public final ObserverSupport<OnSongDetailsObserver> observers_songDetails = new ObserverSupport<>();

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
        listener_addDownloadManager(this);
    }

    public void listener_addDownloadManager(AppModel.DownloadObserver observer) {
        model().downloadObservers.add(observer);
    }

    public void listener_removeDownloadManager(AppModel.DownloadObserver observer) {
        model().downloadObservers.remove(observer);
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


    public String getCloudName() {
        String version = model().usingService(CloudConfigurationManager.class).getProperty("version");
        return "Kodi "+ (version == null?"":version);
    }

    public void offlineMode(boolean enabled) {

        boolean wasValue = isOfflineModeEnabled();

        if (wasValue == enabled){
            return;
        }

        model().usingService(SettingManager.class).set(Settings.MODE_OFFLINE, enabled);
        Event.send(this, Events.OFFLINE_MODE_CHANGED, enabled);
        data_sources.invalidate();
        data_range_folder.invalidateAll();
    }

    public boolean isOfflineModeEnabled() {
        return model().usingService(SettingManager.class).get(Settings.MODE_OFFLINE);
    }

    public void function_updateActivePlaylist(FilePointer filePointer, boolean append) {
       if (!append) {
           model().usingService(Player.class).playlist_clearAndAdd(filePointer);
       }else {
           model().usingService(Player.class).playlist_add(filePointer);
       }
    }

    public Player player(){
        return model().usingService(Player.class);
    }

    @Override
    public void onSongFileDownloadDone(final SongFile songFile) {
        if (songFile.getFilePointer().details != null) return;
        fetchValue(SongDetailsExtractIfNeeded.class, songFile, new NoOpValueAdapter<SongDetails>(), new ValueObserver<SongDetails>() {
            @Override
            public void onSuccess(final SongDetails value) {
                songFile.getFilePointer().details = value;
                observers_songDetails.notify(new Closure<OnSongDetailsObserver, Void>() {
                    @Override
                    public Void execute(OnSongDetailsObserver arg) {
                        arg.onDetails(songFile.getFilePointer(), value);
                        return null;
                    }
                });
            }

            @Override
            public void onFail(Throwable exception) {
                processException(exception);
            }

        });
    }

    public interface OnSongDetailsObserver {
        public void onDetails(FilePointer pointer, SongDetails songDetails);
    }
}
