package team.monroe.org.takeaway.manage;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.media.MediaPlayer;
import android.util.Property;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateInterpolator;

import org.monroe.team.corebox.log.L;

import java.io.IOException;

import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.SongFile;

public class SongManager implements MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnPreparedListener {

    private final L.Logger mL;
    private final MediaPlayer mMediaPlayer;
    private SongFile mSongFile;
    private final Observer mObserver;
    private float mVolumeFraction = 0;
    private ObjectAnimator mFadeAnimator;
    private float mVolume;

    public SongManager(String driverName, Observer mObserver) {
        this.mObserver = mObserver;
        this.mMediaPlayer = new MediaPlayer();
        this.mMediaPlayer.setOnCompletionListener(this);
        this.mMediaPlayer.setOnBufferingUpdateListener(this);
        this.mMediaPlayer.setOnErrorListener(this);
        this.mMediaPlayer.setOnInfoListener(this);
        this.mMediaPlayer.setOnSeekCompleteListener(this);
        this.mMediaPlayer.setOnPreparedListener(this);
        mL = L.create("MEDIA_PLAYER_DRIVE."+driverName);
    }

    public synchronized void setup(SongFile songFile){
        mSongFile = songFile;
        if (mSongFile.isReady()){
            prepare();
        }
    }

    private void prepare() {
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
        mMediaPlayer.reset();
        mObserver.onSongPlayComplete(this, mSongFile);
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
        float volume = mVolume * mVolumeFraction;
        mL.d("Volume: "+volume);
        mMediaPlayer.setVolume(volume, volume);
    }

    public void play(float volume) {
        mFadeAnimator = ObjectAnimator.ofFloat(this,"volumeFraction",0.2f, 1f);
        mFadeAnimator.setDuration(1000 * 10);
        mFadeAnimator.setInterpolator(new AccelerateInterpolator(0.9f));
        mVolume = volume;
        setVolumeFraction(0.2f);
        mMediaPlayer.start();
        mFadeAnimator.start();
    }

    public static interface Observer {
        void onCriticalError(SongManager songManager, Exception e);
        void onSongBroken(SongManager songManager, SongFile mSongFile);
        void onSongPreparedToPlay(SongManager songManager, SongFile mSongFile);
        void onSongPlayComplete(SongManager songManager, SongFile mSongFile);
    }

}
