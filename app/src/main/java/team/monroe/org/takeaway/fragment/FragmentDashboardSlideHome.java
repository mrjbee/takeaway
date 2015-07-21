package team.monroe.org.takeaway.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;

import org.monroe.team.android.box.app.ActivitySupport;
import org.monroe.team.android.box.app.ApplicationSupport;
import org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceController;
import org.monroe.team.android.box.data.Data;
import org.monroe.team.android.box.event.Event;
import org.monroe.team.corebox.utils.Closure;

import java.util.List;

import team.monroe.org.takeaway.App;
import team.monroe.org.takeaway.R;
import team.monroe.org.takeaway.manage.Events;
import team.monroe.org.takeaway.manage.Player;
import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.Playlist;
import team.monroe.org.takeaway.presentations.PlaylistAbout;
import team.monroe.org.takeaway.presentations.SongDetails;
import team.monroe.org.takeaway.view.FormatUtils;
import team.monroe.org.takeaway.view.SeekProgressView;

import static org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceControllerBuilder.alpha;
import static org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceControllerBuilder.animateAppearance;
import static org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceControllerBuilder.combine;
import static org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceControllerBuilder.duration_constant;
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
    private SeekProgressView mSongSeekProgressView;
    private ApplicationSupport.PeriodicalAction mSongProgressUpdateAction;
    private AppearanceController ac_NowPlayingSongControl;
    private Data.DataChangeObserver<List<PlaylistAbout>> observer_recentPlaylist;
    private List<PlaylistAbout> mPlaylistResentList;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_dashboard_home_slide;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mSongPlayBtn = view(R.id.action_now_playing_play, ImageButton.class);
        mSongSeekProgressView = view(R.id.progress_now_playing, SeekProgressView.class);
        mSongSeekProgressView.setProgress(1, SeekProgressView.AnimationSpeed.NO_ANIMATION);
        mSongSeekProgressView.setSeekListener(new SeekProgressView.SeekListener() {
            @Override
            public void onSeekStart(SeekProgressView seekProgressView, float progress) {
                dashboard().requestScreenChangeByTouchEnabled(false);
                mSongProgressUpdateAction.stop();
            }

            @Override
            public void onSeekStop(SeekProgressView seekProgressView, float progress) {
                dashboard().requestScreenChangeByTouchEnabled(true);
                application().player().seekTo(progress);
            }

            @Override
            public void onSeek(SeekProgressView seekProgressView, float progress) {
            }
        });

        mSongPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action_songActionButton();
            }
        });
        view(R.id.action_show_playlist).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action_showPlaylistButton();
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

        view_check(R.id.check_offline_only_error).setChecked(application().function_offlineMode());
        view_check(R.id.check_offline_only_error).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                application().function_offlineMode(isChecked);
            }
        });
    }

    private void action_showPlaylistButton() {
        dashboard().openNavigationDrawer();
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
        mSongProgressUpdateAction.start(500, 1000);
        update_SongProgress(false);

        Event.subscribeOnEvent(activity(), this, Events.OFFLINE_MODE_CHANGED, new Closure<Boolean, Void>() {
            @Override
            public Void execute(Boolean arg) {
                view_check(R.id.check_offline_only_error).setChecked(arg);
                return null;
            }
        });
        observer_recentPlaylist = new Data.DataChangeObserver<List<PlaylistAbout>>() {
            @Override
            public void onDataInvalid() {
                fetch_recentPlaylist();
            }
            @Override
            public void onData(List<PlaylistAbout> playlistAbouts) {}
        };
        application().data_recentPlaylist.addDataChangeObserver(observer_recentPlaylist);
        fetch_recentPlaylist();
    }

    private void fetch_recentPlaylist() {
        application().data_recentPlaylist.fetch(true, activity().observe_data(new ActivitySupport.OnValue<List<PlaylistAbout>>() {
            @Override
            public void action(List<PlaylistAbout> playlistAbouts) {
                update_recentPlaylistUI(playlistAbouts);
            }
        }));
    }

    private void update_recentPlaylistUI(List<PlaylistAbout> playlistAbouts) {
        if (getActivity() == null)return;
        mPlaylistResentList = playlistAbouts;
        if (mPlaylistResentList == null || mPlaylistResentList.isEmpty()){
            view(R.id.panel_playlists).setVisibility(View.GONE);
            return;
        }else {
            view(R.id.panel_playlists).setVisibility(View.VISIBLE);
        }
        ViewGroup playlistContentView = view(R.id.panel_playlists_content, ViewGroup.class);
        playlistContentView.removeAllViews();
        for (final PlaylistAbout playlistAbout : mPlaylistResentList) {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View recentPlaylistView = inflater.inflate(R.layout.item_recent_playlist, playlistContentView, false);
            ((TextView)recentPlaylistView.findViewById(R.id.item_caption)).setText(playlistAbout.title);
            ((TextView)recentPlaylistView.findViewById(R.id.item_description)).setText(playlistAbout.songCount +" songs");
            recentPlaylistView.findViewById(R.id.action_item).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    action_onPlaylistSelected(playlistAbout);
                }
            });
            playlistContentView.addView(recentPlaylistView);

        }
    }

    private void action_onPlaylistSelected(PlaylistAbout playlistAbout) {
        application().function_playlistRestore(playlistAbout, activity().observe(new ActivitySupport.OnValue<Playlist>() {
            @Override
            public void action(Playlist playlist) {
                if (activity() != null){
                    application().player().playlist_set(playlist);
                }
            }
        }));
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
            mSongSeekProgressView.setProgress(progress, progress == 0 ? SeekProgressView.AnimationSpeed.SLOW : SeekProgressView.AnimationSpeed.NORMAL);
        } else {
            mSongSeekProgressView.setProgress(progress, SeekProgressView.AnimationSpeed.NO_ANIMATION);
        }
    }


    @Override
    public void onStop() {
        super.onStop();
        application().player().removePlayerListener(this);
        application().data_recentPlaylist.removeDataChangeObserver(observer_recentPlaylist);
        mSongProgressUpdateAction.stop();
        Event.unSubscribeFromEvents(getActivity(), this);
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

    @Override
    public void onCurrentSongSeekCompleted() {
        mSongProgressUpdateAction.start(0, 1000);
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
