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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import team.monroe.org.takeaway.manage.CloudConfigurationManager;
import team.monroe.org.takeaway.manage.CloudConnectionManager;
import team.monroe.org.takeaway.manage.Events;
import team.monroe.org.takeaway.manage.Player;
import team.monroe.org.takeaway.manage.Settings;
import team.monroe.org.takeaway.presentations.AwarePath;
import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.Playlist;
import team.monroe.org.takeaway.presentations.PlaylistAbout;
import team.monroe.org.takeaway.presentations.SongDetails;
import team.monroe.org.takeaway.presentations.SongFile;
import team.monroe.org.takeaway.presentations.Source;
import team.monroe.org.takeaway.presentations.SourceConnectionStatus;
import team.monroe.org.takeaway.uc.CloudCheckConnection;
import team.monroe.org.takeaway.uc.PathGetContent;
import team.monroe.org.takeaway.uc.PlaylistLoad;
import team.monroe.org.takeaway.uc.SourceGetAll;
import team.monroe.org.takeaway.uc.PlaylistAboutGetAll;
import team.monroe.org.takeaway.uc.PlaylistSave;
import team.monroe.org.takeaway.uc.SongDetailsExtractIfNeeded;

public class App extends ApplicationSupport<AppModel> implements AppModel.DownloadObserver{

    static {
        L.setup(new AndroidLogImplementation());
    }

    public PersistRangeDataProvider<AwarePath, List<FilePointer>> data_range_folder;
    public Data<List<Source>> data_sources;
    public Data<List<PlaylistAbout>> data_recentPlaylist;
    public final ObserverSupport<OnSongDetailsObserver> observers_songDetails = new ObserverSupport<>();
    public final ObserverSupport<OnPlaylistSaveObserver> observers_playlistSave = new ObserverSupport<>();

    @Override
    protected void onPostCreate() {
        super.onPostCreate();
        data_sources = new Data<List<Source>>(model()) {
            @Override
            protected List<Source> provideData() {
                return model().execute(SourceGetAll.class, null);
            }
        };

        data_range_folder = new PersistRangeDataProvider<AwarePath, List<FilePointer>>() {
            @Override
            protected Data<List<FilePointer>> buildData(final AwarePath filePointer) {
                return new Data<List<FilePointer>>(model()) {
                    @Override
                    protected List<FilePointer> provideData() {
                        return model().execute(PathGetContent.class, filePointer);
                    }
                };
            }

            @Override
            protected String convertToStringKey(AwarePath filePointer) {
                return filePointer.getSource().id+":"+filePointer.getRelativePath();
            }
        };

        data_recentPlaylist = new Data<List<PlaylistAbout>>(model()) {
            @Override
            protected List<PlaylistAbout> provideData() {
                List<PlaylistAbout> allSaved = model().execute(PlaylistAboutGetAll.class,null);
                if (allSaved.isEmpty()) return Collections.emptyList();
                Collections.sort(allSaved, new Comparator<PlaylistAbout>() {
                    @Override
                    public int compare(PlaylistAbout lhs, PlaylistAbout rhs) {
                        return lhs.title.compareTo(rhs.title);
                    }
                });
                List<PlaylistAbout> answer = new ArrayList<>();
                for (int i=0; i < 3 && i<allSaved.size(); i++){
                    answer.add(allSaved.get(i));
                }
                return answer;
            }
        };

        model().usingService(CloudConnectionManager.class).startWatcher();
        listener_addDownloadManager(this);
        player().addPlayerListener(new Player.PlayerListenerSupport(){
            @Override
            public void onPlaylistChanged(final Playlist playlist) {
                if (playlist.autosave && playlist.isSaveRequired()){
                    function_playlistSave(playlist, new ValueObserver<Void>() {
                        @Override
                        public void onSuccess(Void value) {
                            observers_playlistSave.notify(new Closure<OnPlaylistSaveObserver, Void>() {
                                @Override
                                public Void execute(OnPlaylistSaveObserver arg) {
                                    arg.onSave(playlist);
                                    return null;
                                }
                            });
                        }

                        @Override
                        public void onFail(Throwable exception) {
                            observers_playlistSave.notify(new Closure<OnPlaylistSaveObserver, Void>() {
                                @Override
                                public Void execute(OnPlaylistSaveObserver arg) {
                                    arg.onSaveRequired(playlist);
                                    return null;
                                }
                            });
                        }
                    });
                }else if (playlist.isSaveRequired()){
                    observers_playlistSave.notify(new Closure<OnPlaylistSaveObserver, Void>() {
                        @Override
                        public Void execute(OnPlaylistSaveObserver arg) {
                            arg.onSaveRequired(playlist);
                            return null;
                        }
                    });
                }

            }
        });
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

    public boolean function_cloudConfigured() {
        return model().usingService(CloudConfigurationManager.class).get() != null;
    }

    public CloudConfigurationManager.Configuration function_cloudConfiguration() {
        return model().usingService(CloudConfigurationManager.class).get();
    }

    public void function_cloudConfiguration(CloudConfigurationManager.Configuration configuration, ValueObserver<SourceConnectionStatus> observer) {
        fetchValue(CloudCheckConnection.class, configuration, new NoOpValueAdapter<SourceConnectionStatus>(), observer);
    }

    public CloudConnectionManager.ConnectionStatus function_cloudConnectionStatus() {
        return model().usingService(CloudConnectionManager.class).getStatus();
    }


    public String function_cloudName() {
        String version = model().usingService(CloudConfigurationManager.class).getProperty("version");
        return "Kodi "+ (version == null?"":version);
    }

    public void function_offlineMode(boolean enabled) {

        boolean wasValue = function_offlineMode();

        if (wasValue == enabled){
            return;
        }

        model().usingService(SettingManager.class).set(Settings.MODE_OFFLINE, enabled);
        Event.send(this, Events.OFFLINE_MODE_CHANGED, enabled);
        data_sources.invalidate();
        data_range_folder.invalidateAll();
    }

    public boolean function_offlineMode() {
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

    public void function_playlistSave(Playlist playlist, ValueObserver<Void> observer) {
        fetchValue(PlaylistSave.class, playlist, new NoOpValueAdapter<Void>(), observer);
    }

    public void function_playlistRestore(PlaylistAbout playlistAbout, ValueObserver<Playlist> observer) {
        fetchValue(PlaylistLoad.class, playlistAbout.id, new NoOpValueAdapter<Playlist>() ,observer);
    }

    public interface OnSongDetailsObserver {
        public void onDetails(FilePointer pointer, SongDetails songDetails);
    }

    public interface OnPlaylistSaveObserver {
        public void onSave(Playlist playlist);
        public void onSaveRequired(Playlist playlist);
    }
}
