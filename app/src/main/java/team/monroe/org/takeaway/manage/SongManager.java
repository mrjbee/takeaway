package team.monroe.org.takeaway.manage;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.media.MediaPlayer;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import org.monroe.team.android.box.app.ui.animation.AnimatorListenerSupport;
import org.monroe.team.corebox.log.L;

import java.io.IOException;

import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.SongFile;

public class SongManager implements MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnPreparedListener {

    private final L.Logger log;
    private final MediaPlayer mMediaPlayer;
    private SongFile mSongFile;
    private final Observer mObserver;
    private float mVolumeFraction = 0.2f;
    private ObjectAnimator mFadeAnimator;

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
        mSongFile = songFile;
        if (mSongFile.isReady()){
            prepare();
        }
    }

    private void prepare() {
        mMediaPlayer.reset();
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
    public void onPrepared(MediaPlayer mp) {
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
    public void onCompletion(MediaPlayer mp) {
        //TODO: might be more tricky if song less then 5 seconds ;) as then fade animator going to be fadeout and no completion
        if (mFadeAnimator!= null && mFadeAnimator.isRunning()){
            mFadeAnimator.cancel();
        }else {
            mMediaPlayer.reset();
            mObserver.onSongPlayComplete(this, mSongFile);
        }
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

    public void play(boolean fadeIn) {
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
    }

    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    public void stop() {
        log.i("Request to stop");
        if (mFadeAnimator != null && mFadeAnimator.isRunning()){
            mFadeAnimator.cancel();
        }
        mFadeAnimator = ObjectAnimator.ofFloat(this,"volumeFraction",mVolumeFraction, 0.2f);
        mFadeAnimator.setDuration(1000 * 5);
        mFadeAnimator.setInterpolator(new DecelerateInterpolator(0.9f));
        mFadeAnimator.addListener(new AnimatorListenerSupport(){
            @Override
            public void onAnimationEnd(Animator animation) {
                stopAndReset();
            }
        });
        setVolumeFraction(mVolumeFraction);
        mMediaPlayer.start();
        mFadeAnimator.start();
    }

    private void stopAndReset() {
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        mObserver.onSongPlayComplete(this, mSongFile);
    }

    public static interface Observer {
        void onCriticalError(SongManager songManager, Exception e);
        void onSongBroken(SongManager songManager, SongFile mSongFile);
        void onSongPreparedToPlay(SongManager songManager, SongFile mSongFile);
        void onSongPlayComplete(SongManager songManager, SongFile mSongFile);
    }

}
