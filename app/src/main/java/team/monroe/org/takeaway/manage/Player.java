package team.monroe.org.takeaway.manage;

import org.monroe.team.android.box.data.Data;
import org.monroe.team.corebox.app.Model;
import org.monroe.team.corebox.log.L;
import org.monroe.team.corebox.services.BackgroundTaskManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.Playlist;
import team.monroe.org.takeaway.uc.GetFileContent;

public class Player {

    private static final  L.Logger LOG = L.create("PLAYER.PLAYLIST");

    private final Model mModel;
    private final Object mPlaylistResourceMonitor = new Object();

    public Data<Playlist> data_active_playlist;
    private Playlist mCurrentPlaylist = null;
    private Playlist mPlaylistUnderBuild = null;
    private List<PlaylistUpdateJob> mPlaylistBuildJobList = new ArrayList<>();
    private BackgroundTaskManager.BackgroundTask<Void> mBackgroundUpdateTask;


    public Player(Model model) {
        this.mModel = model;
        mCurrentPlaylist = createEmptyPlayList();
        data_active_playlist = new Data<Playlist>(model) {
            @Override
            protected Playlist provideData() {
                return getPlaylist();
            }
        };
    }

    private Playlist createEmptyPlayList() {
        return new Playlist("No Name","no_name", new ArrayList<FilePointer>());
    }

    public Playlist getPlaylist(){
        LOG.d("Playlist requested");
        synchronized (mPlaylistResourceMonitor) {
            if (mCurrentPlaylist != null) {
                LOG.d("Playlist returned [songs]: " + mCurrentPlaylist);
                return mCurrentPlaylist;
            }
            try {
                LOG.d("Play list awaiting...");
                mPlaylistResourceMonitor.wait();
            } catch (InterruptedException e) {
            }
            LOG.d("Playlist returned [songs]: " + mCurrentPlaylist);
            return mCurrentPlaylist;
        }
    }

    public synchronized void clearAndAddToPlayList(FilePointer filePointer) {
        LOG.d("New playlist requested...");
        if (mBackgroundUpdateTask != null){
            mBackgroundUpdateTask.cancel();
            mBackgroundUpdateTask = null;
        }
        synchronized (mPlaylistResourceMonitor) {
            mPlaylistUnderBuild = createEmptyPlayList();
            mPlaylistBuildJobList.clear();
            mCurrentPlaylist = null;
            data_active_playlist.invalidate();
        }
        addUpdateJob(filePointer);
    }


    public synchronized void addToPlayList(FilePointer filePointer) {
        synchronized (mPlaylistResourceMonitor) {
            LOG.d("Add to existing playlist requested...");
            if (mPlaylistUnderBuild == null && mCurrentPlaylist == null) {
                mPlaylistUnderBuild = createEmptyPlayList();
            } else if (mPlaylistUnderBuild == null && mCurrentPlaylist != null) {
                mPlaylistUnderBuild = mCurrentPlaylist.duplicate();
            }

            mCurrentPlaylist = null;
            data_active_playlist.invalidate();
        }
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
                    Player.this.onUpdateTaskSuccess();
                }
                @Override
                public void onFails(Exception e) {
                    Player.this.onUpdateTaskFailed(e);
                }
            });
        }
    }

    private synchronized void onUpdateTaskFailed(Exception e) {
        LOG.d("Playlist creation failed: %s", e.getClass().getName());

        if (e instanceof UpdateCancelException) return;

        mBackgroundUpdateTask = null;
        if (mPlaylistBuildJobList.isEmpty()){
            synchronized (mPlaylistResourceMonitor) {
                LOG.d("Playlist [empty] created notification");
                mCurrentPlaylist = createEmptyPlayList();
                mBackgroundUpdateTask = null;
                mPlaylistResourceMonitor.notifyAll();
            }
        }else {
            LOG.d("Playlist creation next task ...");
            startNextJob();
        }
    }

    private synchronized void onUpdateTaskSuccess() {
        LOG.d("Playlist creation finished");
        mBackgroundUpdateTask = null;
        if (mPlaylistBuildJobList.isEmpty()){
            synchronized (mPlaylistResourceMonitor) {
                LOG.d("Playlist created notification");
                mCurrentPlaylist = mPlaylistUnderBuild;
                mBackgroundUpdateTask = null;
                mPlaylistResourceMonitor.notifyAll();
            }
        }else {
            LOG.d("Playlist creation next task ...");
            startNextJob();
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
            Player.this.mPlaylistUnderBuild.songList.addAll(songList);
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
