package team.monroe.org.takeaway.fragment;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import org.monroe.team.android.box.app.ApplicationSupport;
import org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceController;
import org.monroe.team.android.box.utils.DisplayUtils;

import team.monroe.org.takeaway.App;
import team.monroe.org.takeaway.R;
import team.monroe.org.takeaway.manage.Player;
import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.Playlist;
import team.monroe.org.takeaway.presentations.SongDetails;
import team.monroe.org.takeaway.view.FormatUtils;
import team.monroe.org.takeaway.view.ProgressView;

import static org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceControllerBuilder.alpha;
import static org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceControllerBuilder.animateAppearance;
import static org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceControllerBuilder.combine;
import static org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceControllerBuilder.duration_constant;
import static org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceControllerBuilder.heightSlide;
import static org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceControllerBuilder.interpreter_accelerate;
import static org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceControllerBuilder.interpreter_overshot;
import static org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceControllerBuilder.rotate;
import static org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceControllerBuilder.scale;

public class FragmentDashboardSlideHome extends FragmentDashboardSlide implements Player.PlayerListener, App.OnSongDetailsObserver {

    private FilePointer mFilePointer;
    private boolean mReadyToPlay = false;
    private ImageButton mSongPlayBtn;
    private AppearanceController ac_SongPlayButton;
    private AppearanceController ac_NowPlayingCard;
    private ProgressView mSongProgressView;
    private ApplicationSupport.PeriodicalAction mSongProgressUpdateAction;
    private AppearanceController ac_NowPlayingSongControl;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_dashboard_home_slide;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mSongPlayBtn = view(R.id.action_now_playing_play, ImageButton.class);
        mSongProgressView = view(R.id.progress_now_playing, ProgressView.class);
        mSongProgressView.setProgress(1, ProgressView.AnimationSpeed.NO_ANIMATION);

        mSongPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action_songActionButton();
            }
        });

        view(R.id.action_now_playing_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (application().player().hasNext()){
                    application().player().playNext();
                }
            }
        });

        view(R.id.action_now_playing_prev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (application().player().hasPrev()){
                    application().player().playPrev();
                }
            }
        });
        ac_SongPlayButton = combine(
                animateAppearance(mSongPlayBtn, rotate(0f, 30f))
                    .showAnimation(duration_constant(300), interpreter_overshot())
                    .hideAnimation(duration_constant(200), interpreter_accelerate(0.8f)),
                animateAppearance(mSongPlayBtn, scale(1f,0f))
                    .showAnimation(duration_constant(600), interpreter_overshot())
                    .hideAnimation(duration_constant(400), interpreter_accelerate(0.8f))
                );

        ac_NowPlayingCard = animateAppearance(view(R.id.panel_now_playing), alpha(1f,0f))
                .showAnimation(duration_constant(300), interpreter_accelerate(0.9f))
                .hideAnimation(duration_constant(200), interpreter_accelerate(0.8f))
                .hideAndGone()
                .build();

        ac_NowPlayingSongControl = animateAppearance(view(R.id.panel_now_playing_progress), alpha(1f,0f))
                        .showAnimation(duration_constant(500), interpreter_accelerate(0.9f))
                        .hideAnimation(duration_constant(500), interpreter_accelerate(0.8f))
                        .build();


        ac_NowPlayingCard.hideWithoutAnimation();
        ac_SongPlayButton.hideWithoutAnimation();
    }

    private void action_songActionButton() {
        ac_SongPlayButton.hide();
        if (application().player().isSongPlaying()){
            application().player().pause();
        }else {
            application().player().resume();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        application().observers_songDetails.add(this);
        application().player().addPlayerListener(this);
        FilePointer filePointer = application().player().getCurrentSong();
        update_currentSong(filePointer, false);
        mReadyToPlay = !application().player().isBuffering();
        visibility_playControls(mReadyToPlay, false);
        if (application().player().isSongPlaying()){
            onCurrentSongPlay();
        } else {
            onCurrentSongStop();
        }

        mSongProgressUpdateAction = application().preparePeriodicalAction(new Runnable() {
            @Override
            public void run() {
                update_SongProgress(true);
            }
        });
        mSongProgressUpdateAction.start(500, 500);
        update_SongProgress(false);
    }

    private void update_SongProgress(boolean animate) {
        if (activity() == null) return;
        long[] durationAndPosition = application().player().getDurationAndPosition();
        float progress = 0;

        String durationText = "00:00";
        String progressText = "00:00";

        if (durationAndPosition[0] != -1){
            progress = (float) ((double)durationAndPosition[1]/ (double)durationAndPosition[0]);
            durationText = FormatUtils.toTimeString(durationAndPosition[0]);
            progressText = FormatUtils.toTimeString(durationAndPosition[1]);
        }

        view_text(R.id.text_now_playing_time_position).setText(progressText);
        view_text(R.id.text_now_playing_time_duration).setText(durationText);

        if (animate) {
            mSongProgressView.setProgress(progress, progress == 0 ? ProgressView.AnimationSpeed.SLOW : ProgressView.AnimationSpeed.NORMAL);
        } else {
            mSongProgressView.setProgress(progress, ProgressView.AnimationSpeed.NO_ANIMATION);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        application().player().removePlayerListener(this);
        mSongProgressUpdateAction.stop();
    }

    private synchronized void update_currentSong(FilePointer filePointer, boolean animate) {
        mReadyToPlay = false;
        if(filePointer == null){
            mFilePointer = null;
            visibility_nowPlayingSong(false, animate);
            return;
        } else if (mFilePointer == null && filePointer != null) {
            mFilePointer = filePointer;
            update_songUI();
            visibility_nowPlayingSong(true, animate);
            return;
        }
        mFilePointer = filePointer;
        update_songUI();
        visibility_playControls(false, animate);
    }

    private void visibility_playControls(boolean visible, boolean animate) {
        if (animate){
            if (visible){
                ac_NowPlayingSongControl.show();
            }else {
                ac_NowPlayingSongControl.hide();
            }
        }else {
            if (visible){
                ac_NowPlayingSongControl.showWithoutAnimation();
            }else {
                ac_NowPlayingSongControl.hideWithoutAnimation();
            }
        }
    }

    private void visibility_nowPlayingSong(boolean visible, boolean animate) {
        if (animate){
            if (visible){
                ac_NowPlayingCard.show();
            }else {
                ac_NowPlayingCard.hide();
            }
        }else {
            if (visible){
                ac_NowPlayingCard.showWithoutAnimation();
            }else {
                ac_NowPlayingCard.hideWithoutAnimation();
            }
        }
    }

    private void update_songUI() {
        if (mFilePointer == null)return;
        view_text(R.id.text_now_playing_artist).setText(FormatUtils.getArtistString(mFilePointer,getResources()));
        view_text(R.id.text_now_playing_title).setText(FormatUtils.getSongTitle(mFilePointer, getResources()));
    }


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
        update_currentSong(filePointer, true);
    }

    @Override
    public void onCurrentSongReady(FilePointer filePointer) {
        mReadyToPlay = true;
        visibility_playControls(true, true);
    }

    @Override
    public void onCurrentSongPlay() {
        final int drawable_icon = R.drawable.android_pause_round_pink;
        updateButtonIcon(drawable_icon);
    }

    @Override
    public void onCurrentSongStop() {
        final int drawable_icon = R.drawable.android_play_round_pink;
        updateButtonIcon(drawable_icon);
    }

    private void updateButtonIcon(final int drawable_icon) {
        mSongPlayBtn.setImageResource(drawable_icon);
        ac_SongPlayButton.show();
    }

    @Override
    public void onDetails(FilePointer pointer, SongDetails songDetails) {
        if (pointer.equals(mFilePointer)){
            mFilePointer.details = songDetails;
            update_songUI();
        }
    }

}
