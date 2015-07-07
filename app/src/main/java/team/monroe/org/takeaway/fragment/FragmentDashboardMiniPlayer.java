package team.monroe.org.takeaway.fragment;

import android.os.Bundle;
import android.view.View;

import org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceController;
import org.monroe.team.android.box.app.ui.animation.apperrance.SceneDirector;
import org.monroe.team.android.box.utils.DisplayUtils;

import static org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceControllerBuilder.*;

import team.monroe.org.takeaway.R;
import team.monroe.org.takeaway.manage.Player;
import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.Playlist;

public class FragmentDashboardMiniPlayer extends FragmentDashboardActivity implements Player.PlayerListener {

    private FilePointer mFilePointer;
    private AppearanceController ac_Content_showFromRight;
    private AppearanceController ac_Content_showFromLeft;
    private boolean mReadyToPlay = false;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_mini_player;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ac_Content_showFromRight = animateAppearance(view(R.id.panel_song_content), xSlide(0f, DisplayUtils.screenWidth(getResources())))
                .showAnimation(duration_constant(300), interpreter_overshot())
                .hideAnimation(duration_constant(300), interpreter_accelerate(0.8f))
                .build();
        ac_Content_showFromLeft = animateAppearance(view(R.id.panel_song_content), xSlide(0f, -DisplayUtils.screenWidth(getResources())))
                .showAnimation(duration_constant(300), interpreter_overshot())
                .hideAnimation(duration_constant(300), interpreter_accelerate(0.8f))
                .build();

        ac_Content_showFromLeft.showWithoutAnimation();
        ac_Content_showFromRight.showWithoutAnimation();
    }

    @Override
    public void onStart() {
        super.onStart();
        application().player().addPlayerListener(this);
        FilePointer filePointer = application().player().getCurrentSong();
        update_currentSong(filePointer, !application().player().isBuffering(), false);
    }

    @Override
    public void onStop() {
        super.onStop();
        application().player().removePlayerListener(this);
    }

    private synchronized void update_currentSong(FilePointer filePointer, boolean readyToPlay, boolean animate) {
        mReadyToPlay = readyToPlay;
        if (filePointer == null){
            dashboard().visibility_MiniPlayer(false, animate);
            mFilePointer = filePointer;
            return;
        }

        if (mFilePointer == null){
            mFilePointer = filePointer;
            update_songUI();
            dashboard().visibility_MiniPlayer(true, animate);
        }else {
            if (mFilePointer.equals(filePointer)){
                update_songUI();
                return;
            }
            mFilePointer = filePointer;
            //TODO: internal animation here
            SceneDirector.scenario()
                        .hide(ac_Content_showFromRight)
                        .then()
                            .action(new Runnable() {
                                @Override
                                public void run() {
                                    update_songUI();
                                }
                            })
                            .then()
                                .show(ac_Content_showFromRight)
                        .play();
        }

    }

    private void update_songUI() {
        if (mFilePointer == null)return;
        view_text(R.id.text_song_caption).setText(mFilePointer.getNormalizedTitle());
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
        update_currentSong(filePointer, false, true);
    }

    @Override
    public void onCurrentSongReady(FilePointer filePointer) {
        update_currentSong(filePointer, true, true);
    }
}
