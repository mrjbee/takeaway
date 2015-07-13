package team.monroe.org.takeaway.fragment;

import android.animation.Animator;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import org.monroe.team.android.box.app.ApplicationSupport;
import org.monroe.team.android.box.app.ui.SlideTouchGesture;
import org.monroe.team.android.box.app.ui.animation.AnimatorListenerSupport;
import org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceController;
import static org.monroe.team.android.box.app.ui.animation.apperrance.SceneDirector.*;

import org.monroe.team.android.box.utils.DisplayUtils;

import static org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceControllerBuilder.*;

import team.monroe.org.takeaway.App;
import team.monroe.org.takeaway.R;
import team.monroe.org.takeaway.manage.Player;
import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.Playlist;
import team.monroe.org.takeaway.presentations.SongDetails;
import team.monroe.org.takeaway.view.ProgressView;

public class FragmentDashboardMiniPlayer extends FragmentDashboardActivity implements Player.PlayerListener, App.OnSongDetailsObserver {

    private FilePointer mFilePointer;


    private AppearanceController ac_Content_showFromRight;
    private AppearanceController ac_Content_showFromLeft;
    private Position mPosition = Position.NORMAL;
    private boolean mReadyToPlay = false;
    private ImageButton mSongPlayBtn;
    private AppearanceController ac_SongPlayButton;
    private ProgressView mSongProgressView;
    private ApplicationSupport.PeriodicalAction mSongProgressUpdateAction;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_mini_player;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final View mSongContentPanel = view(R.id.panel_song_content);
        mSongPlayBtn = view(R.id.action_song_play, ImageButton.class);
        mSongProgressView = view(R.id.progress_song, ProgressView.class);

        mSongProgressView.setProgress(1, ProgressView.AnimationSpeed.NO_ANIMATION);

        mSongPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action_songActionButton();
            }
        });
        ac_SongPlayButton = animateAppearance(mSongPlayBtn, scale(1f,0f))
                .showAnimation(duration_constant(300), interpreter_overshot())
                .hideAnimation(duration_constant(200), interpreter_accelerate(0.8f))
                .hideAndGone()
                .build();

        ac_Content_showFromRight = animateAppearance(mSongContentPanel, xSlide(0f, DisplayUtils.screenWidth(getResources())))
                .showAnimation(duration_constant(500), interpreter_overshot())
                .hideAnimation(duration_constant(300), interpreter_accelerate(0.8f))
                .build();

        ac_Content_showFromLeft = animateAppearance(mSongContentPanel, xSlide(0f, -DisplayUtils.screenWidth(getResources())))
                .showAnimation(duration_constant(500), interpreter_overshot())
                .hideAnimation(duration_constant(400), interpreter_accelerate(0.8f))
                .build();

        ac_SongPlayButton.hideWithoutAnimation();

        final float slideLimit = DisplayUtils.screenWidth(getResources()) / 2f;
        mSongContentPanel.setOnTouchListener(new SlideTouchGesture(slideLimit, SlideTouchGesture.Axis.X) {

            @Override
            protected float applyFraction() {
                return 0.6f;
            }

            @Override
            protected void onApply(float x, float y, float slideValue, float fraction) {
                float sign = getDirectionSign(slideValue);
                if (sign < 0){
                    if (application().player().hasNext()){
                        ac_Content_showFromLeft.hideAndCustomize(new AppearanceController.AnimatorCustomization() {
                            @Override
                            public void customize(Animator animator) {
                                animator.addListener(new AnimatorListenerSupport(){
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        application().player().playNext();
                                        mPosition = Position.LEFT;
                                    }
                                });
                            }
                        });
                        return;
                    }
                }else {
                    if (application().player().hasPrev()){
                        ac_Content_showFromRight.hideAndCustomize(new AppearanceController.AnimatorCustomization() {
                            @Override
                            public void customize(Animator animator) {
                                animator.addListener(new AnimatorListenerSupport(){
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        application().player().playPrev();
                                        mPosition = Position.RIGHT;
                                    }
                                });
                            }
                        });
                        return;
                    }
                }
                onCancel(x, y, slideValue, fraction);
            }

            @Override
            protected void onCancel(float x, float y, float slideValue, float fraction) {
                float sign = getDirectionSign(slideValue);
                if (sign < 0){
                    ac_Content_showFromRight.show();
                }else {
                    ac_Content_showFromLeft.show();
                }
            }

            @Override
            protected void onProgress(float x, float y, float slideValue, float fraction) {
                float sign = getDirectionSign(slideValue);
                mSongContentPanel.setTranslationX(slideLimit * fraction * sign);
            }

            private float getDirectionSign(float slideValue) {
                if (slideValue > 0 ){
                    return -1;
                }
                return 1;
            }
        });
        ac_Content_showFromLeft.showWithoutAnimation();
        ac_Content_showFromRight.showWithoutAnimation();
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
        update_songBufferUI();

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
        if (durationAndPosition[0] != -1){
            progress = (float) ((double)durationAndPosition[1]/ (double)durationAndPosition[0]);
        }
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
            dashboard().visibility_MiniPlayer(false, animate);
            mFilePointer = null;
            return;
        } else if (mFilePointer == null && filePointer != null) {
            mFilePointer = filePointer;
            dashboard().visibility_MiniPlayer(true, animate);
            mPosition = Position.NORMAL;
            update_songUI();
            update_songBufferUI();
            return;
        }

        mFilePointer = filePointer;

        switch (mPosition){
            case NORMAL:
                scenario()
                        .action(action_updatePosition(Position.IN_MOTION_TO_HIDE))
                        .hide(ac_Content_showFromRight)
                        .then()
                        .action(action_updatePosition(Position.RIGHT))
                        .action(new Runnable() {
                            @Override
                            public void run() {
                                update_songUI();
                                update_songBufferUI();
                            }
                        })
                        .then()
                            .action(action_updatePosition(Position.IN_MOTION_TO_SHOW))
                            .show(ac_Content_showFromRight)
                            .then()
                                .action(action_updatePosition(Position.NORMAL))
                        .play();
                return;
            case LEFT:
                scenario()
                        .action_hide_without_animation(ac_Content_showFromRight)
                        .action(new Runnable() {
                            @Override
                            public void run() {
                                update_songUI();
                                update_songBufferUI();
                            }
                        })
                        .action(action_updatePosition(Position.IN_MOTION_TO_SHOW))
                        .show(ac_Content_showFromRight)
                            .then()
                                .action(action_updatePosition(Position.NORMAL))
                        .play();
                return;

            case RIGHT:
                scenario()
                        .action_hide_without_animation(ac_Content_showFromLeft)
                        .action(new Runnable() {
                            @Override
                            public void run() {
                                update_songUI();
                                update_songBufferUI();
                            }
                        })
                        .action(action_updatePosition(Position.IN_MOTION_TO_SHOW))
                        .show(ac_Content_showFromLeft)
                            .then()
                                .action(action_updatePosition(Position.NORMAL))
                    .play();
                return;
            case IN_MOTION_TO_SHOW:
                scenario()
                        .action_wait(400)
                        .then()
                            .action_hide_without_animation(ac_Content_showFromLeft)
                            .action(new Runnable() {
                                @Override
                                public void run() {
                                    update_songUI();
                                    update_songBufferUI();
                                }
                            })
                            .action(action_updatePosition(Position.IN_MOTION_TO_SHOW))
                            .show(ac_Content_showFromLeft)
                                .then()
                                    .action(action_updatePosition(Position.NORMAL))
                        .play();
        }
    }

    private Runnable action_updatePosition(final Position position) {
        return new Runnable() {
            @Override
            public void run() {
                mPosition = position;
            }
        };
    }

    private void update_songUI() {
        if (mFilePointer == null)return;
        view_text(R.id.text_song_caption).setText(mFilePointer.getNormalizedTitle());
        if (mFilePointer.details != null){
            if (mFilePointer.details.title != null){
                view_text(R.id.text_song_caption).setText(mFilePointer.details.title);
            }
        }
    }

    private void update_songBufferUI() {
        if (mReadyToPlay){
            view(R.id.progress_song_buffering).setVisibility(View.GONE);
            view(R.id.image_song_cover).setVisibility(View.VISIBLE);
        }else {
            view(R.id.progress_song_buffering).setVisibility(View.VISIBLE);
            view(R.id.image_song_cover).setVisibility(View.GONE);
        }
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
        if (mPosition != Position.IN_MOTION_TO_HIDE){
            update_songBufferUI();
        }
    }

    @Override
    public void onCurrentSongPlay() {
        final int drawable_icon = R.drawable.android_stop;
        updateButtonIcon(drawable_icon);
    }

    @Override
    public void onCurrentSongStop() {
        final int drawable_icon = R.drawable.android_play_round;
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


    private enum Position {
       NORMAL, IN_MOTION_TO_HIDE, IN_MOTION_TO_SHOW, LEFT, RIGHT
    }

}
