package team.monroe.org.takeaway.manage;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.media.MediaPlayer;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import org.monroe.team.android.box.app.ui.animation.AnimatorListenerSupport;
import org.monroe.team.corebox.log.L;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.SongFile;

public class SongManager implements MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnPreparedListener {

    private final L.Logger log;
    private final MediaPlayer mMediaPlayer;
    private SongFile mSongFile;
    private final Observer mObserver;
    private float mVolumeFraction = 0.2f;
    private ObjectAnimator mFadeAnimator;
    private boolean mPrepared = false;
    private Timer mNextPlayTimer;

    public SongManager(String driverName, Observer mObserver) {
        this.mObserver = mObserver;
        this.mMediaPlayer = new MediaPlayer();
        this.mMediaPlayer.setOnCompletionListener(this);
        this.mMediaPlayer.setOnBufferingUpdateListener(this);
        this.mMediaPlayer.setOnErrorListener(this);
        this.mMediaPlayer.setOnInfoListener(this);
        this.mMediaPlayer.setOnSeekCompleteListener(this);
        this.mMediaPlayer.setOnPreparedListener(this);
        log = L.create("MEDIA_PLAYER_DRIVE."+driverName);
    }

    public synchronized void setup(SongFile songFile){
        log.i("Setup song: "+songFile.getFilePointer().relativePath);
        mPrepared = false;
        mMediaPlayer.reset();
        mSongFile = songFile;

        if (mSongFile.isReady()){
            prepare();
        }

    }

    private synchronized void prepare() {
        if (mSongFile.isBroken()){
            mObserver.onSongBroken(this, mSongFile);
            return;
        }

        try {
            mMediaPlayer.setDataSource(mSongFile.getDataSourcePath());
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            mObserver.onSongBroken(this, mSongFile);
        } catch (Exception e){
            mObserver.onCriticalError(this, e);
        }
    }

    public synchronized void onSongReady(FilePointer filePointer) {
        if (mSongFile != null && filePointer.equals(mSongFile.getFilePointer())){
            prepare();
        }
    }

    @Override
    public synchronized void onPrepared(MediaPlayer mp) {
        if (mSongFile == null){
            return;
        }
        mPrepared = true;
        mObserver.onSongPreparedToPlay(this, mSongFile);
    }

    private long getSongTimeLeft() {
        long msBeforeEnd = -1;
        long mSongDuration = mMediaPlayer.getDuration();
        if (mSongDuration != -1){
            msBeforeEnd = mSongDuration - mMediaPlayer.getCurrentPosition();
        }
        return msBeforeEnd;
    }

    @Override
    public synchronized void onCompletion(MediaPlayer mp) {
        mObserver.onSongEnd(this);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
    }

    public float getVolumeFraction() {
        return mVolumeFraction;
    }

    public void setVolumeFraction(float volumeFraction) {
        this.mVolumeFraction = volumeFraction;
        mMediaPlayer.setVolume(volumeFraction, volumeFraction);
    }

    public synchronized void play(boolean fadeIn) {
        log.i("Request to play");
        if (mFadeAnimator != null && mFadeAnimator.isRunning()){
            mFadeAnimator.cancel();
        }
        if (fadeIn) {
            mFadeAnimator = ObjectAnimator.ofFloat(this, "volumeFraction", mVolumeFraction, 1f);
            mFadeAnimator.setStartDelay(1000 * 3);
            mFadeAnimator.setDuration(1000 * 5);
            mFadeAnimator.setInterpolator(new AccelerateInterpolator(0.9f));
            setVolumeFraction(mVolumeFraction);
            mFadeAnimator.start();
        }else {
            setVolumeFraction(1f);
        }
        mMediaPlayer.start();
        nextSongTimer_schedule();
    }

    private synchronized void nextSongTimer_schedule() {
        log.i("[NEXT_SONG_TIME] Scheduling timer ... " );
        if(mNextPlayTimer != null){
           nextSongTimer_cancel();
           return;
        }
        long nextSongTime = getSongTimeLeft();
        log.i("[NEXT_SONG_TIME] Delay = " + nextSongTime );
        if (nextSongTime  == -1) return;

        mNextPlayTimer = new Timer("next_song_timer", true);
        long fadeOutDuration = 1000 * 5 * 2;
        if (nextSongTime < fadeOutDuration){
            log.i("[NEXT_SONG_TIME] No time doing now ");
            nextSongTimer_onTime();
        }else {
            log.i("[NEXT_SONG_TIME] Scheduling after = "+(nextSongTime - fadeOutDuration));
            mNextPlayTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    nextSongTimer_onTime();
                }
            }, nextSongTime - fadeOutDuration);
        }

    }

    private synchronized void nextSongTimer_onTime() {
        log.i("[NEXT_SONG_TIME] On time = "+ mNextPlayTimer);
        if (mNextPlayTimer == null) return;
        nextSongTimer_cancel();
        mObserver.onSongNearEnd(this);
    }

    private synchronized void nextSongTimer_cancel() {
        log.i("[NEXT_SONG_TIME] Cancel = "+ mNextPlayTimer);
        if(mNextPlayTimer == null){
            return;
        }
        mNextPlayTimer.cancel();
        mNextPlayTimer.purge();
        mNextPlayTimer = null;
    }

    public synchronized boolean isPlaying() {
        if (!mPrepared) return false;
        return mMediaPlayer.isPlaying();
    }

    public synchronized void stop() {
        log.i("Request to stop");
        pause();
        if (mPrepared) {
            mMediaPlayer.seekTo(0);
        }
    }

    public synchronized void pause() {
        log.i("Request to pause");
        if (mFadeAnimator != null && mFadeAnimator.isRunning()){
            mFadeAnimator.cancel();
        }
        if (mPrepared) {
            if (mMediaPlayer.isPlaying()){
                mMediaPlayer.pause();
            }
        }
        nextSongTimer_cancel();
    }



    public synchronized void release() {
        if (mSongFile == null) return;
        nextSongTimer_cancel();
        log.i("Request to release");
        if (mFadeAnimator != null && mFadeAnimator.isRunning()){
            mFadeAnimator.cancel();
        }
        actualStop();
    }

    public synchronized boolean releaseWithFade() {
        log.i("Request to release with fade");
        nextSongTimer_cancel();
        if (!isPlaying()){
            log.i("Request to release with fade [not playing]");
            release();
            return false;
        }

        if (mFadeAnimator != null && mFadeAnimator.isRunning()){
            mFadeAnimator.cancel();
        }

        mFadeAnimator = ObjectAnimator.ofFloat(this,"volumeFraction",mVolumeFraction, 0.05f);
        mFadeAnimator.setDuration(1000 * 5);
        mFadeAnimator.setInterpolator(new DecelerateInterpolator(0.9f));
        mFadeAnimator.addListener(new AnimatorListenerSupport(){
            @Override
            public void onAnimationEnd(Animator animation) {
                actualStop();
            }
        });

        setVolumeFraction(mVolumeFraction);
        mFadeAnimator.start();
        return true;
    }

    private synchronized void actualStop() {
        if (mPrepared) {
            if (mMediaPlayer.isPlaying()){
                mMediaPlayer.stop();
            }
        }
        mObserver.onSongPlayStop(this, mSongFile);
        mSongFile = null;
        mPrepared = false;
    }

    public synchronized boolean isSetupFor(SongFile song) {
        return mSongFile == song;
    }

    public void resume() {
        mVolumeFraction = 1f;
        if (mPrepared){
            mMediaPlayer.start();
            nextSongTimer_schedule();
        }
    }

    public static interface Observer {
        void onCriticalError(SongManager songManager, Exception e);
        void onSongBroken(SongManager songManager, SongFile mSongFile);
        void onSongPreparedToPlay(SongManager songManager, SongFile mSongFile);
        void onSongPlayStop(SongManager songManager, SongFile mSongFile);
        void onSongEnd(SongManager songManager);
        void onSongNearEnd(SongManager songManager);
    }

}
