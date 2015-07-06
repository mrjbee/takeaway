package team.monroe.org.takeaway.manage;

import android.content.Context;

import org.monroe.team.android.box.event.Event;
import org.monroe.team.corebox.app.Model;
import org.monroe.team.corebox.log.L;
import org.monroe.team.corebox.services.BackgroundTaskManager;
import org.monroe.team.corebox.utils.Closure;
import org.monroe.team.corebox.utils.Lists;
import org.monroe.team.corebox.utils.P;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.Playlist;
import team.monroe.org.takeaway.presentations.SongFile;
import team.monroe.org.takeaway.uc.GetFileContent;
import team.monroe.org.takeaway.uc.GetSoundFiles;

public class Player implements SongManager.Observer {

    private final static L.Logger log = L.create("PLAYER.PLAY");
    private final Model mModel;
    private final PlaylistController mPlaylistController = new PlaylistController();
    private final List<PlayerListener> mListenerList = new ArrayList<>();

    private FilePointer mCurrentSongFilePointer;
    private SongFile mCurrentPlayingSong;
    private SongFile mSongAwaitingToRelease;
    private List<SongFile> mSongPlayQueue;
    private ArrayList<SongManager> mSongManagerPool = new ArrayList<>();

    public Player(Context context, Model model) {
        this.mModel = model;
        Event.subscribeOnEvent(context, this, Events.FILE_PREPARED, new Closure<P<FilePointer, Boolean>, Void>() {
            @Override
            public Void execute(P<FilePointer, Boolean> filePrepared) {
                onFileReady(filePrepared.first, filePrepared.second);
                return null;
            }
        });
        mSongManagerPool.add(new SongManager("PRIMARY", this));
        mSongManagerPool.add(new SongManager("SECONDARY", this));
    }

    private synchronized void onFileReady(FilePointer filePointer, boolean readyStatus) {
        for (SongManager songManager : mSongManagerPool) {
            songManager.onSongReady(filePointer);
        }

    }

    public synchronized void addPlayerListener(PlayerListener listener){
        mListenerList.add(listener);
    }

    public synchronized void removePlayerListener(PlayerListener listener) {
        mListenerList.remove(listener);
    }

    private synchronized void notifyListeners(final Closure <PlayerListener, Void> notificationAction){
        mModel.getResponseHandler().post(new Runnable() {
            @Override
            public void run() {
                for (PlayerListener playerListener : mListenerList) {
                    notificationAction.execute(playerListener);
                }
            }
        });
    }

    public Playlist getPlaylist(){
       return mPlaylistController.getPlaylist();
    }

    public void clearAndAddToPlayList(FilePointer filePointer) {
        mPlaylistController.clearAndAddToPlayList(filePointer);
    }


    public void addToPlayList(FilePointer filePointer) {
        mPlaylistController.addToPlayList(filePointer);
    }

    public synchronized void play(FilePointer filePointer) {
        mCurrentSongFilePointer = filePointer;
        List<FilePointer> nearestPlaySongList = generateNextPlayQueue(filePointer);
        mModel.execute(GetSoundFiles.class, nearestPlaySongList, new Model.BackgroundResultCallback<List<SongFile>>() {
            @Override
            public void onResult(List<SongFile> response) {
                process_playQueue(response);
            }

            @Override
            public void onFails(final Throwable e) {
                notifyListeners(new Closure<PlayerListener, Void>() {
                    @Override
                    public Void execute(PlayerListener arg) {
                        arg.onError(e);
                        return null;
                    }
                });
            }
        });
    }


    private synchronized void process_playQueue(List<SongFile> response) {
        if (!response.get(0).getFilePointer().equals(mCurrentSongFilePointer)) {
            //Old response
            return;
        }

        boolean currentPlayingSongReleaseRequired = true;
        if (mSongPlayQueue != null) {
            for (SongFile songFile : mSongPlayQueue) {
                if (mCurrentPlayingSong != songFile && response.indexOf(songFile) == -1) {
                    songFile.release();
                }else if (mCurrentPlayingSong == songFile){
                    currentPlayingSongReleaseRequired = false;
                }
            }
        }

        mSongPlayQueue = response;
        if (currentPlayingSongReleaseRequired){
            if (mSongAwaitingToRelease != null && mSongAwaitingToRelease != mCurrentPlayingSong){
                mSongAwaitingToRelease.release();
            }
            mSongAwaitingToRelease = mCurrentPlayingSong;
        }

        mCurrentPlayingSong = response.get(0);
        if (mCurrentPlayingSong instanceof SongFile.NotAvailableSongFile) {
            notifyListeners(new Closure<PlayerListener, Void>() {
                @Override
                public Void execute(PlayerListener arg) {
                    arg.onUnavailableFile(mCurrentPlayingSong.getFilePointer());
                    return null;
                }
            });
            return;
        }

        SongManager topSongManger = mSongManagerPool.get(0);
        topSongManger.setup(mCurrentPlayingSong);
    }


    private synchronized List<FilePointer> generateNextPlayQueue(FilePointer filePointer) {
        List<FilePointer> answer = new ArrayList<>(4);
        answer.add(filePointer);
        Playlist playlist = getPlaylist();
        if (playlist == null){
            return answer;
        }

        List<FilePointer> songsInPlaylist = playlist.songList;
        int songIndex = songsInPlaylist.indexOf(filePointer);

        addNextSongIfExists(answer, songsInPlaylist, songIndex, 1);
        addPrevSongIfExists(answer, songsInPlaylist, songIndex);
        addNextSongIfExists(answer, songsInPlaylist, songIndex, 2);
        addNextSongIfExists(answer, songsInPlaylist, songIndex, 3);
        return answer;
    }

    private boolean addPrevSongIfExists(List<FilePointer> answer, List<FilePointer> songsInPlaylist, int songIndex) {
        FilePointer prevSong = getPrevSong(songIndex, songsInPlaylist);
        if (prevSong != null){
            answer.add(prevSong);
            return true;
        }
        return false;
    }

    private boolean addNextSongIfExists(List<FilePointer> answer, List<FilePointer> songsInPlaylist, int songIndex, int nextStepCount) {
        FilePointer nextSong = getNextSong(songIndex, nextStepCount, songsInPlaylist);
        if (nextSong != null){
            answer.add(nextSong);
            return true;
        }
        return false;
    }

    private FilePointer getNextSong(int songIndex, int stepCount, List<FilePointer> songsInPlaylist) {
        if (Lists.getLastIndex(songsInPlaylist) < (songIndex + stepCount)){
            return null;
        }
        return songsInPlaylist.get(songIndex + stepCount);
    }

    private FilePointer getPrevSong(int songIndex, List<FilePointer> songsInPlaylist) {
        if (songIndex == 0)return null;
        return songsInPlaylist.get(songIndex-1);
    }

    @Override
    public synchronized void onCriticalError(SongManager songManager, Exception e) {
        log.e("Song manager raise critical error", e);
    }

    @Override
    public synchronized void onSongBroken(SongManager songManager, SongFile mSongFile) {

    }

    @Override
    public synchronized void onSongPreparedToPlay(SongManager songManager, SongFile mSongFile) {
        log.d("Song ready to play");
        if (songManager == mSongManagerPool.get(0)){
            log.d("Start playing song");
            //top player ready start to play
            songManager.play(mSongManagerPool.get(1).isPlaying());
            mSongManagerPool.add(mSongManagerPool.remove(0));
            if (mSongManagerPool.get(0).isPlaying()){
                mSongManagerPool.get(0).stop();
            }
        }
    }

    @Override
    public synchronized void onSongPlayComplete(SongManager songManager, SongFile mSongFile) {
        if (mSongFile == mSongAwaitingToRelease){
            mSongAwaitingToRelease.release();
        }
        //Switch this to top
        mSongManagerPool.add(0, mSongManagerPool.remove(mSongManagerPool.indexOf(songManager)));
    }

    public static interface PlayerListener {
        void onPlaylistCalculation();
        void onPlaylistChanged(Playlist playlist);
        void onError(Throwable e);
        void onUnavailableFile(FilePointer filePointer);
    }


    private class PlaylistController{

       private final  L.Logger LOG = L.create("PLAYER.PLAYLIST");

       private Playlist mCurrentPlaylist = null;
       private Playlist mPlaylistUnderBuild = null;
       private List<PlaylistUpdateJob> mPlaylistBuildJobList = new ArrayList<>();
       private BackgroundTaskManager.BackgroundTask<Void> mBackgroundUpdateTask;


       public PlaylistController() {
           mCurrentPlaylist = createEmptyPlayList();
       }

       private Playlist createEmptyPlayList() {
           return new Playlist("Playlist","no_name", new ArrayList<FilePointer>());
       }

       public Playlist getPlaylist(){
           LOG.d("Playlist returned [songs]: " + mCurrentPlaylist);
           return mCurrentPlaylist;
       }

       public synchronized void clearAndAddToPlayList(FilePointer filePointer) {
           LOG.d("New playlist requested...");
           if (mBackgroundUpdateTask != null){
               mBackgroundUpdateTask.cancel();
               mBackgroundUpdateTask = null;
           }
           mPlaylistUnderBuild = createEmptyPlayList();
           mPlaylistBuildJobList.clear();
           mCurrentPlaylist = null;
           notifyListeners(new Closure<PlayerListener, Void>() {
               @Override
               public Void execute(PlayerListener arg) {
                   arg.onPlaylistCalculation();
                   return null;
               }
           });
           addUpdateJob(filePointer);
       }


       public synchronized void addToPlayList(FilePointer filePointer) {
           LOG.d("Add to existing playlist requested...");
           if (mPlaylistUnderBuild == null && mCurrentPlaylist == null) {
               mPlaylistUnderBuild = createEmptyPlayList();
           } else if (mPlaylistUnderBuild == null && mCurrentPlaylist != null) {
               mPlaylistUnderBuild = mCurrentPlaylist.duplicate();
           }
           mCurrentPlaylist = null;
           notifyListeners(new Closure<PlayerListener, Void>() {
               @Override
               public Void execute(PlayerListener arg) {
                   arg.onPlaylistCalculation();
                   return null;
               }
           });
           addUpdateJob(filePointer);
       }

       private void addUpdateJob(FilePointer filePointer) {
           LOG.d("Start playlist creation.");
           mPlaylistBuildJobList.add(new PlaylistUpdateJob(filePointer));
           startNextJob();
       }

       private void startNextJob() {
           if (mBackgroundUpdateTask == null) {
               mBackgroundUpdateTask = mModel.usingService(BackgroundTaskManager.class).execute(mPlaylistBuildJobList.remove(0), new BackgroundTaskManager.TaskCompletionNotificationObserver<Void>() {
                   @Override
                   public void onSuccess(Void aVoid) {
                       Player.this.mPlaylistController.onUpdateTaskSuccess();
                   }
                   @Override
                   public void onFails(Exception e) {
                       Player.this.mPlaylistController.onUpdateTaskFailed(e);
                   }
               });
           }
       }

       private synchronized void onUpdateTaskFailed(Exception e) {
           LOG.d("Playlist creation failed: %s", e.getClass().getName());

           if (e instanceof UpdateCancelException) return;

           mBackgroundUpdateTask = null;
           if (mPlaylistBuildJobList.isEmpty()){
               LOG.d("Playlist [empty] created notification");
               mCurrentPlaylist = createEmptyPlayList();
               mBackgroundUpdateTask = null;
               notifyListeners(new Closure<PlayerListener, Void>() {
                   @Override
                   public Void execute(PlayerListener arg) {
                       arg.onPlaylistChanged(mCurrentPlaylist);
                       return null;
                   }
               });
           }else {
               LOG.d("Playlist creation next task ...");
               startNextJob();
           }
       }

       private synchronized void onUpdateTaskSuccess() {
           LOG.d("Playlist creation finished");
           mBackgroundUpdateTask = null;
           if (mPlaylistBuildJobList.isEmpty()){
               LOG.d("Playlist created notification");
               mCurrentPlaylist = mPlaylistUnderBuild;
               mBackgroundUpdateTask = null;
               notifyListeners(new Closure<PlayerListener, Void>() {
                   @Override
                   public Void execute(PlayerListener arg) {
                       arg.onPlaylistChanged(mCurrentPlaylist);
                       return null;
                   }
               });
           }else {
               LOG.d("Playlist creation next task ...");
               startNextJob();
           }
       }
   }

    class PlaylistUpdateJob implements Callable<Void> {

        private final FilePointer filePointer;

        PlaylistUpdateJob(FilePointer filePointer) {
            this.filePointer = filePointer;
        }

        @Override
        public Void call() throws Exception {
            List<FilePointer> songList = new ArrayList<>();
            explore(filePointer, songList);
            checkAndStop();
            Player.this.mPlaylistController.mPlaylistUnderBuild.songList.addAll(songList);
            return null;
        }
        private void explore(FilePointer filePointer, List<FilePointer> songList) {
            if (filePointer.type == FilePointer.Type.FILE){
                songList.add(filePointer);
            }else {
                checkAndStop();
                List<FilePointer> files = Player.this.mModel.execute(GetFileContent.class,filePointer);
                for (FilePointer file : files) {
                    explore(file, songList);
                }
            }
        }

        private void checkAndStop() {
            if (Thread.currentThread().isInterrupted()){
                throw new UpdateCancelException();
            }
        }
    }

   class UpdateCancelException extends RuntimeException{}

}
