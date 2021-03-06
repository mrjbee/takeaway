package team.monroe.org.takeaway.manage;

import android.content.Context;

import org.monroe.team.corebox.app.Model;
import org.monroe.team.corebox.log.L;
import org.monroe.team.corebox.services.BackgroundTaskManager;
import org.monroe.team.corebox.utils.Closure;
import org.monroe.team.corebox.utils.DateUtils;
import org.monroe.team.corebox.utils.Lists;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import team.monroe.org.takeaway.AppModel;
import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.Playlist;
import team.monroe.org.takeaway.presentations.SongFile;
import team.monroe.org.takeaway.uc.PathGetContent;
import team.monroe.org.takeaway.uc.SoundFileGet;

public class Player implements SongManager.Observer, AppModel.DownloadObserver {

    private final static L.Logger log = L.create("PLAYER.PLAY");
    private final Model mModel;
    private final PlaylistController mPlaylistController = new PlaylistController();
    private final List<PlayerListener> mListenerList = new ArrayList<>();

    private FilePointer mCurrentSongFilePointer;
    private SongFile mCurrentPlayingSong;
    private SongFile mSongAwaitingToRelease;
    private List<SongFile> mSongPlayQueue;
    private ArrayList<SongManager> mSongManagerPool = new ArrayList<>();
    private boolean mBuffering = false;
    private SongPlayState mSongPlayState = SongPlayState.NOT_USED;

    public Player(Context context, AppModel model) {
        this.mModel = model;
        model.downloadObservers.add(this);
        mSongManagerPool.add(new SongManager("PRIMARY", this));
        mSongManagerPool.add(new SongManager("SECONDARY", this));
    }

    @Override
    public synchronized void onSongFileDownloadDone(SongFile songFile) {
        for (SongManager songManager : mSongManagerPool) {
            songManager.onSongReady(songFile.getFilePointer());
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

    public Playlist playlist(){
       return mPlaylistController.getPlaylist();
    }


    public void playlist_set(Playlist playlist) {
        mPlaylistController.setPlaylist(playlist);
    }

    public void playlist_clearAndAdd(FilePointer filePointer) {
        mPlaylistController.clearAndAddToPlayList(filePointer);
    }


    public void playlist_add(FilePointer filePointer) {
        mPlaylistController.addToPlayList(filePointer);
    }

    public void playlist_updateOrder(Playlist playlist, List<FilePointer> fileList) {
        mPlaylistController.updatePlaylistOrder(playlist, fileList);
    }

    public boolean playlist_removeFrom(Playlist playlist, FilePointer filePointer) {
        if (filePointer == mCurrentSongFilePointer){
           return false;
        }
        mPlaylistController.removeFromPlaylist(playlist, filePointer);
        return true;
    }

    public synchronized boolean resume() {
        log.i("Resume song");
        SongManager songManager = mSongManagerPool.get(1);
        mSongPlayState = SongPlayState.PLAY;
        if (songManager.isSetupFor(mCurrentPlayingSong)){
            //resume
            log.i("Resume song [actual resuming]");
            songManager.resume();
            notifyListeners(new Closure<PlayerListener, Void>() {
                @Override
                public Void execute(PlayerListener arg) {
                    arg.onCurrentSongPlay();
                    return null;
                }
            });
            return true;
        } else {
            if (mCurrentPlayingSong == null){
                log.i("Resume song [nothing to resume]");
                return false;
            }else {
                //Waiting for callback
                log.i("Resume song [postponed resume]. Waiting song for playing");
                return true;
            }
        }
    }

    public synchronized void pause() {
        log.i("Pause song");
        mSongManagerPool.get(0).release();
        mSongManagerPool.get(1).pause();
        mSongPlayState = SongPlayState.STOP;
        notifyListeners(new Closure<PlayerListener, Void>() {
            @Override
            public Void execute(PlayerListener arg) {
                arg.onCurrentSongStop();
                return null;
            }
        });
    }

    private synchronized void playOnPlaylistChange() {

        if (mPlaylistController.hasSong(mCurrentSongFilePointer)){
            return;
        }

        FilePointer filePointer = mPlaylistController.getFirstSong();
        updateCurrent(filePointer, false);
    }

    public synchronized void updateCache() {
        if (mCurrentSongFilePointer == null) return;
        log.i("Play song");
        List<FilePointer> nearestPlaySongList = generateNextPlayQueue(mCurrentSongFilePointer);
        mModel.execute(SoundFileGet.class, nearestPlaySongList, new Model.BackgroundResultCallback<List<SongFile>>() {
            @Override
            public void onResult(List<SongFile> response) {
                process_applySongCache(response);
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
    public synchronized void play(FilePointer filePointer) {
        updateCurrent(filePointer, true);
    }

    public synchronized void updateCurrent(FilePointer filePointer, boolean play) {
        log.i("Play song");
        mCurrentSongFilePointer = filePointer;
        List<FilePointer> nearestPlaySongList = generateNextPlayQueue(filePointer);
        mModel.execute(SoundFileGet.class, nearestPlaySongList, new Model.BackgroundResultCallback<List<SongFile>>() {
            @Override
            public void onResult(List<SongFile> response) {
                process_playQueue(response, true);
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

    private synchronized void process_playQueue(List<SongFile> response, boolean autoplay) {
        log.i("Play song asynch [Song File List Ready]");
        if (process_applySongCache(response)) return;

        boolean currentPlayingSongReleaseRequired = response.indexOf(mCurrentPlayingSong) == -1;
        if (currentPlayingSongReleaseRequired){
            if (mSongAwaitingToRelease != null && mSongAwaitingToRelease != mCurrentPlayingSong){
                mSongAwaitingToRelease.release();
            }
            mSongAwaitingToRelease = mCurrentPlayingSong;
        }

        mCurrentPlayingSong = response.get(0);
        log.i("Play song asynch [current song] = "+mCurrentPlayingSong.getFilePointer().relativePath);
        if (mCurrentPlayingSong instanceof SongFile.NotAvailableSongFile) {
            log.i("Play song asynch [current song bad] = "+mCurrentPlayingSong.getFilePointer().relativePath);
            mBuffering = false;
            mSongPlayState = SongPlayState.STOP;
            notifyListeners(new Closure<PlayerListener, Void>() {
                @Override
                public Void execute(PlayerListener arg) {
                    arg.onUnavailableFile(mCurrentPlayingSong.getFilePointer());
                    arg.onCurrentSongStop();
                    return null;
                }
            });
            return;
        }else {
            mBuffering = true;
            notifyListeners(new Closure<PlayerListener, Void>() {
                @Override
                public Void execute(PlayerListener arg) {
                    arg.onCurrentSongChanged(mCurrentPlayingSong.getFilePointer());
                    return null;
                }
            });
        }


        SongManager topSongManger = mSongManagerPool.get(0);
        topSongManger.release();
        if (autoplay) {
            mSongPlayState = SongPlayState.PLAY;
        }
        topSongManger.setup(mCurrentPlayingSong);
        mSongManagerPool.add(mSongManagerPool.remove(0));


    }

    private synchronized boolean process_applySongCache(List<SongFile> response) {
        if (!response.get(0).getFilePointer().equals(mCurrentSongFilePointer)) {
            //Old response
            log.i("Play song asynch [Song List To Old]");
            return true;
        }

        if (mSongPlayQueue != null) {
            for (SongFile oldSongFile : mSongPlayQueue) {
                if (mCurrentPlayingSong != oldSongFile && response.indexOf(oldSongFile) == -1) {
                    oldSongFile.release();
                }
            }
        }
        mSongPlayQueue = response;
        return false;
    }

    @Override
    public synchronized void onSongPreparedToPlay(SongManager songManager, final SongFile mSongFile) {
        log.i("Play song [prepared] = " + mSongFile.getFilePointer().relativePath);
        if (songManager == mSongManagerPool.get(1)){
            mBuffering = false;
            notifyListeners(new Closure<PlayerListener, Void>() {
                @Override
                public Void execute(PlayerListener arg) {
                    arg.onCurrentSongReady(mSongFile.getFilePointer());
                    return null;
                }
            });

            log.i("Play song [prepared]. Start playing = " + mSongFile.getFilePointer().relativePath);
            //top player ready start to play
            if (isSongPlaying()) {
                songManager.play(mSongManagerPool.get(0).isPlaying());
                //Moved from process_queue
                notifyListeners(new Closure<PlayerListener, Void>() {
                    @Override
                    public Void execute(PlayerListener arg) {
                        arg.onCurrentSongPlay();
                        return null;
                    }
                });
            }
            if (mSongManagerPool.get(0).isPlaying()){
                mSongManagerPool.get(0).releaseWithFade();
            }else {
                mSongManagerPool.get(0).release();
            }
        }
    }

    private synchronized List<FilePointer> generateNextPlayQueue(FilePointer filePointer) {
        List<FilePointer> answer = new ArrayList<>(4);
        answer.add(filePointer);
        Playlist playlist = playlist();
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
    public synchronized void onSongPlayStop(SongManager songManager, SongFile mSongFile) {
        if (mSongFile != null && mSongFile == mSongAwaitingToRelease){
            mSongAwaitingToRelease.release();
            mSongAwaitingToRelease = null;
        }
    }

    @Override
    public void onSongEnd(SongManager songManager) {
        log.i("On song end");
        if (songManager != mSongManagerPool.get(1)) return;

        if (hasNext()) {
            songManager.release();
            playNext();
        }else {
            pause();
        }
    }

    @Override
    public void onSongNearEnd(SongManager songManager) {
        log.i("On song end");
        if (songManager != mSongManagerPool.get(1)) return;
        playNext();
    }

    @Override
    public void onSongSeekCompleted() {
        notifyListeners(new Closure<PlayerListener, Void>() {
            @Override
            public Void execute(PlayerListener arg) {
                arg.onCurrentSongSeekCompleted();
                return null;
            }
        });
    }

    public synchronized FilePointer getCurrentSong() {
        return (mCurrentPlayingSong ==null) ? null:mCurrentPlayingSong.getFilePointer();
    }

    public synchronized boolean isBuffering() {
        return mBuffering;
    }

    public synchronized boolean hasNext() {
        FilePointer filePointer = mPlaylistController.getSongAfter(mCurrentSongFilePointer);
        return filePointer != null;
    }

    public synchronized boolean playNext() {
        FilePointer filePointer = mPlaylistController.getSongAfter(mCurrentSongFilePointer);
        if (filePointer != null){
            play(filePointer);
            return true;
        }else {
            return false;
        }
    }

    public synchronized boolean hasPrev() {
        FilePointer filePointer = mPlaylistController.getSongBefore(mCurrentSongFilePointer);
        return filePointer != null;
    }


    public synchronized boolean playPrev() {
        FilePointer filePointer = mPlaylistController.getSongBefore(mCurrentSongFilePointer);
        if (filePointer != null){
            play(filePointer);
            return true;
        }else {
            return false;
        }
    }

    public synchronized boolean isSongPlaying() {
        return mSongPlayState == SongPlayState.PLAY;
    }

    public synchronized long[] getDurationAndPosition() {
        SongManager songManager = mSongManagerPool.get(1);
        long[] answer = new long[]{songManager.getDuration(), songManager.getPosition()};
        return answer;
    }

    public synchronized void seekTo(float progress) {
        SongManager songManager = mSongManagerPool.get(1);
        long duration = songManager.getDuration();
        if (duration == -1 || !songManager.seekTo((long) (duration * progress))){
            notifyListeners(new Closure<PlayerListener, Void>() {
                @Override
                public Void execute(PlayerListener arg) {
                    arg.onCurrentSongSeekCompleted();
                    return null;
                }
            });
        }
    }




    public static interface PlayerListener {
        void onPlaylistCalculation();
        void onPlaylistChanged(Playlist playlist);
        void onError(Throwable e);
        void onUnavailableFile(FilePointer filePointer);
        void onCurrentSongChanged(FilePointer filePointer);
        void onCurrentSongReady(FilePointer filePointer);
        void onCurrentSongPlay();
        void onCurrentSongStop();
        void onCurrentSongSeekCompleted();
    }

    public static class PlayerListenerSupport implements PlayerListener{

        @Override
        public void onPlaylistCalculation() {

        }

        @Override
        public void onPlaylistChanged(Playlist playlist) {

        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onUnavailableFile(FilePointer filePointer) {

        }

        @Override
        public void onCurrentSongChanged(FilePointer filePointer) {

        }

        @Override
        public void onCurrentSongReady(FilePointer filePointer) {

        }

        @Override
        public void onCurrentSongPlay() {

        }

        @Override
        public void onCurrentSongStop() {

        }

        @Override
        public void onCurrentSongSeekCompleted() {

        }
    }

    private static enum SongPlayState {
        NOT_USED, STOP, PAUSED, PLAY
    }

    private class PlaylistController{

       private final  L.Logger LOG = L.create("PLAYER.PLAYLIST");

       private Playlist mPlaylistCurrent = null;
       private Playlist mPlaylistUnderBuild = null;
       private List<PlaylistUpdateJob> mPlaylistBuildJobList = new ArrayList<>();
       private BackgroundTaskManager.BackgroundTask<Void> mBackgroundUpdateTask;


        public PlaylistController() {
           mPlaylistCurrent = createEmptyPlayList();
       }

       private Playlist createEmptyPlayList() {
           return new Playlist(DateUtils.msAsString(), null, new ArrayList<FilePointer>());
       }

       public Playlist getPlaylist(){
           LOG.d("Playlist returned [songs]: " + mPlaylistCurrent);
           return mPlaylistCurrent;
       }

        public synchronized void removeFromPlaylist(final Playlist playlist, FilePointer filePointer) {
            LOG.d("Playlist removing requested ...");
            if (mPlaylistCurrent == null || mPlaylistCurrent != playlist){
                LOG.d("Playlist outdated ...");
                return;
            }
            mPlaylistCurrent.songList.remove(filePointer);
            mPlaylistCurrent.dateModified = DateUtils.now().getTime();
            notifyListeners(new Closure<PlayerListener, Void>() {
                @Override
                public Void execute(PlayerListener arg) {
                    arg.onPlaylistChanged(playlist);
                    return null;
                }
            });
            Player.this.updateCache();
        }
       public synchronized void updatePlaylistOrder(final Playlist playlist, List<FilePointer> fileList) {
           LOG.d("Playlist reorder requested...");
           if (mPlaylistCurrent == null || mPlaylistCurrent != playlist){
               LOG.d("Playlist outdated ...");
               return;
           }
           mPlaylistCurrent.songList.clear();
           mPlaylistCurrent.songList.addAll(fileList);
           mPlaylistCurrent.dateModified = DateUtils.now().getTime();
           notifyListeners(new Closure<PlayerListener, Void>() {
               @Override
               public Void execute(PlayerListener arg) {
                   arg.onPlaylistChanged(playlist);
                   return null;
               }
           });
           Player.this.updateCache();
       }

       public synchronized void clearAndAddToPlayList(FilePointer filePointer) {
           LOG.d("New playlist requested...");
           if (mBackgroundUpdateTask != null){
               mBackgroundUpdateTask.cancel();
               mBackgroundUpdateTask = null;
           }
           mPlaylistUnderBuild = createEmptyPlayList();
           mPlaylistBuildJobList.clear();
           mPlaylistCurrent = null;
           notifyListeners(new Closure<PlayerListener, Void>() {
               @Override
               public Void execute(PlayerListener arg) {
                   arg.onPlaylistCalculation();
                   return null;
               }
           });
           addUpdateJob(filePointer);
       }

        public synchronized void setPlaylist(final Playlist playlist) {
            LOG.d("Set existing playlist requested...");
            if (mBackgroundUpdateTask != null){
                mBackgroundUpdateTask.cancel();
            }
            mPlaylistCurrent = null;
            notifyListeners(new Closure<PlayerListener, Void>() {
                @Override
                public Void execute(PlayerListener arg) {
                    arg.onPlaylistCalculation();
                    return null;
                }
            });
            mPlaylistCurrent = playlist;
            notifyListeners(new Closure<PlayerListener, Void>() {
                @Override
                public Void execute(PlayerListener arg) {
                    arg.onPlaylistChanged(playlist);
                    return null;
                }
            });
            playOnPlaylistChange();
        }

       public synchronized void addToPlayList(FilePointer filePointer) {
           LOG.d("Add to existing playlist requested...");
           if (mPlaylistUnderBuild == null && mPlaylistCurrent == null) {
               mPlaylistUnderBuild = createEmptyPlayList();
           } else if (mPlaylistUnderBuild == null && mPlaylistCurrent != null) {
               mPlaylistUnderBuild = mPlaylistCurrent;
               mPlaylistUnderBuild.dateModified = DateUtils.now().getTime();
           }
           mPlaylistCurrent = null;
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

           if (e instanceof UpdateCancelException){
               return;
           }

           mBackgroundUpdateTask = null;
           if (mPlaylistBuildJobList.isEmpty()){
               LOG.d("Playlist [empty] created notification");
               mPlaylistCurrent = createEmptyPlayList();
               mPlaylistUnderBuild = null;
               mBackgroundUpdateTask = null;
               notifyListeners(new Closure<PlayerListener, Void>() {
                   @Override
                   public Void execute(PlayerListener arg) {
                       arg.onPlaylistChanged(mPlaylistCurrent);
                       return null;
                   }
               });
               playOnPlaylistChange();
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
               mPlaylistCurrent = mPlaylistUnderBuild;
               mPlaylistUnderBuild = null;
               mBackgroundUpdateTask = null;
               notifyListeners(new Closure<PlayerListener, Void>() {
                   @Override
                   public Void execute(PlayerListener arg) {
                       arg.onPlaylistChanged(mPlaylistCurrent);
                       return null;
                   }
               });
               playOnPlaylistChange();
           }else {
               LOG.d("Playlist creation next task ...");
               startNextJob();
           }
       }

        public synchronized FilePointer getSongBefore(FilePointer filePointer) {
            if (mPlaylistCurrent == null) {
                return null;
            }
            int index = getPlaylistFileIndex(filePointer);
            if (index == -1) {
                //plalist changed return first
                return getSongFirst();
            }
            index--;
            if (index < 0){
                //TODO: add play loop logic
                return null;
            }
            return mPlaylistCurrent.songList.get(index);
        }

        public synchronized FilePointer getSongAfter(FilePointer filePointer) {
            if (mPlaylistCurrent == null) {
                return null;
            }
            int index = getPlaylistFileIndex(filePointer);
            if (index == -1) {
                //plalist changed return first
                return getSongFirst();
            }
            index++;
            if (index >= mPlaylistCurrent.songList.size()){
                //TODO: add play loop logic
                return null;
            }
            return mPlaylistCurrent.songList.get(index);
        }

        private int getPlaylistFileIndex(FilePointer filePointer) {
            for (int index = 0; index < mPlaylistCurrent.songList.size(); index++) {
                if (mPlaylistCurrent.songList.get(index) == filePointer){
                    return index;
                }
            }
            return -1;
        }

        public  synchronized FilePointer getSongFirst() {
            if (mPlaylistCurrent == null) {
                return null;
            }
            //TODO: shuffle and so on
            return mPlaylistCurrent.songList.get(0);
        }

        public synchronized FilePointer getFirstSong() {
            if (mPlaylistCurrent == null || mPlaylistCurrent.songList.size() == 0) return null;
            return mPlaylistCurrent.songList.get(0);
        }

        public boolean hasSong(FilePointer songToCheck) {
            if (mPlaylistCurrent == null || mPlaylistCurrent.songList.size() == 0) return false;
            return mPlaylistCurrent.songList.indexOf(songToCheck) != -1;
        }



    }

    private synchronized void updateUnderBuildPlaylist(List<FilePointer> songList) {
        Lists.iterateAndRemove(songList, new Closure<Iterator<FilePointer>, Boolean>() {
            @Override
            public Boolean execute(Iterator<FilePointer> arg) {
                if (Player.this.mPlaylistController.mPlaylistUnderBuild.songList.indexOf(arg.next()) != -1) {
                    arg.remove();
                }
                return true;
            }
        });
        Player.this.mPlaylistController.mPlaylistUnderBuild.songList.addAll(songList);
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
            updateUnderBuildPlaylist(songList);
            checkAndStop();
            return null;
        }
        private void explore(FilePointer filePointer, List<FilePointer> songList) {
            if (filePointer.type == FilePointer.Type.FILE){
                songList.add(filePointer);
            }else {
                checkAndStop();
                List<FilePointer> files = Player.this.mModel.execute(PathGetContent.class,filePointer);
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
