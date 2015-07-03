package team.monroe.org.takeaway.manage;

import org.monroe.team.corebox.app.Model;
import org.monroe.team.corebox.log.L;
import org.monroe.team.corebox.services.BackgroundTaskManager;
import org.monroe.team.corebox.utils.Closure;
import org.monroe.team.corebox.utils.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.Playlist;
import team.monroe.org.takeaway.presentations.SongFile;
import team.monroe.org.takeaway.uc.GetFileContent;
import team.monroe.org.takeaway.uc.GetSoundFiles;

public class Player {

    private int MAX_PLAY_PREPARE_QUEUE = 3;

    private final Model mModel;
    private final PlaylistController mPlaylistController = new PlaylistController();
    private final List<PlayerListener> mListenerList = new ArrayList<>();

    private FilePointer mCurrentSongFilePointer;
    private SongFile mCurrentPlayingSound;
    private List<SongFile> mSongPlayQueue;

    public Player(Model model) {
        this.mModel = model;
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

    public synchronized void playNext() {

    }

    private synchronized void process_playQueue(List<SongFile> response) {
        if (!response.get(0).getFilePointer().equals(mCurrentSongFilePointer)) {
            //Old response
            return;
        }

        if (mSongPlayQueue != null) {
            for (SongFile songFile : mSongPlayQueue) {
                if (mCurrentPlayingSound != songFile && response.indexOf(songFile) == -1) {
                    songFile.release();
                }
            }
        }

        mSongPlayQueue = response;

        if (mCurrentPlayingSound != null) {
            //may require to do something with it
        }

        mCurrentPlayingSound = response.get(0);
        if (mCurrentPlayingSound instanceof SongFile.NotAvailableSongFile) {
            notifyListeners(new Closure<PlayerListener, Void>() {
                @Override
                public Void execute(PlayerListener arg) {
                    arg.onUnavailableFile(mCurrentPlayingSound.getFilePointer());
                    return null;
                }
            });
        }
    }

    private synchronized List<FilePointer> generateNextPlayQueue(FilePointer filePointer) {
        List<FilePointer> answer = new ArrayList<>(MAX_PLAY_PREPARE_QUEUE+2);
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
