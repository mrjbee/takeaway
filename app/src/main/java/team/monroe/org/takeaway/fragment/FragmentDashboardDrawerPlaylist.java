package team.monroe.org.takeaway.fragment;

import android.animation.Animator;
import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.monroe.team.android.box.app.ActivitySupport;
import org.monroe.team.android.box.app.ui.GenericListViewAdapter;
import org.monroe.team.android.box.app.ui.GetViewImplementation;
import org.monroe.team.android.box.app.ui.SlideTouchGesture;
import static org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceControllerBuilder.*;

import org.monroe.team.android.box.app.ui.animation.AnimatorListenerSupport;
import org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceController;
import org.monroe.team.android.box.utils.DisplayUtils;
import org.monroe.team.corebox.utils.Closure;

import java.util.ArrayList;
import java.util.List;

import team.monroe.org.takeaway.App;
import team.monroe.org.takeaway.R;
import team.monroe.org.takeaway.manage.Player;
import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.Playlist;
import team.monroe.org.takeaway.presentations.SongDetails;
import team.monroe.org.takeaway.view.DynamicListAdapter;
import team.monroe.org.takeaway.view.DynamicListView;
import team.monroe.org.takeaway.view.FormatUtils;

public class FragmentDashboardDrawerPlaylist extends FragmentDashboardActivity implements Player.PlayerListener, App.OnSongDetailsObserver, DynamicListView.OnElementsSwapListener, App.OnPlaylistSaveObserver {

    private View mLoadingPanel;
    private DynamicListView mItemList;
    private View mNoItemsPanel;
    private GenericListViewAdapter<FilePointer, GetViewImplementation.ViewHolder<FilePointer>> mPlaylistAdapter;
    private TextView mPlaylistText;
    private TextView mSongsText;

    private FilePointer mPlayFilePointer;
    private boolean mSongPlaying = false;
    private boolean mSongBuffering = false;
    private boolean mSwapInProgress = false;
    private Playlist mPlaylist;
    private float mDashWeight;
    private View mPlayListPanel;
    private AppearanceController ac_playlistDetails;
    private boolean mPlaylistDetailsShown = false;
    private View mSavePlalistActionButton;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_drawer_playlist;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mDashWeight = DisplayUtils.dpToPx(400, getResources());
        getFragmentView().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        mPlayListPanel = view(R.id.panel_list);

        mPlaylistText = view_text(R.id.text_playlist_title);
        mSongsText =  view_text(R.id.text_song_count);

        mLoadingPanel = view(R.id.panel_loading);
        mItemList = view(R.id.list_items, DynamicListView.class);
        mItemList.setSwapListener(this);
        mNoItemsPanel = view(R.id.panel_no_items);

        view(R.id.panel_playlist_details).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        mSavePlalistActionButton = view(R.id.action_playlist_save);
        mSavePlalistActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action_onPlaylistSaveExisting();
            }
        });

        ac_playlistDetails = combine(
                    animateAppearance(view(R.id.action_playlist_more), rotate(180f, 0f))
                        .showAnimation(duration_constant(300), interpreter_overshot())
                        .hideAnimation(duration_constant(500), interpreter_decelerate(0.8f)),
                    animateAppearance(view(R.id.panel_playlist_details), ySlide(0, - DisplayUtils.screenHeight(getResources())))
                        .showAnimation(duration_constant(400), interpreter_accelerate(0.8f))
                        .hideAnimation(duration_constant(500), interpreter_decelerate(0.8f))
                        .hideAndGone()
        );

        hide_all();

        view(R.id.action_playlist_more).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action_onPlaylistMore();
            }
        });

        view(R.id.action_playlist_details_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action_onPlaylistSave();
            }
        });

        mPlaylistAdapter = new PlaylistAdapter(activity(), new GetViewImplementation.ViewHolderFactory<GetViewImplementation.ViewHolder<FilePointer>>() {
            @Override
            public GetViewImplementation.ViewHolder<FilePointer> create(final View convertView) {
                return new GetViewImplementation.GenericViewHolder<FilePointer>() {

                    TextView caption = (TextView) convertView.findViewById(R.id.item_caption);
                    TextView description = (TextView) convertView.findViewById(R.id.item_description);
                    View separator = convertView.findViewById(R.id.separator);
                    ImageView imageView = (ImageView) convertView.findViewById(R.id.item_image);
                    View content = convertView.findViewById(R.id.panel_content);
                    AppearanceController ac_content;
                    public FilePointer mFilePointer;


                    @Override
                    public void discoverUI() {
                        ac_content = animateAppearance(content, xSlide(0f, mDashWeight))
                                .showAnimation(duration_constant(800), interpreter_overshot())
                                .hideAnimation(duration_constant(600), interpreter_decelerate(0.8f))
                                .build();
                    }

                    @Override
                    public void update(final FilePointer filePointer, int position) {
                        mFilePointer = filePointer;
                        if (ac_content == null){
                            discoverUI();
                        }

                        ac_content.showWithoutAnimation();
                        imageView.setOnTouchListener(new SlideTouchGesture(mDashWeight/2f, SlideTouchGesture.Axis.X_RIGHT) {

                            @Override
                            protected float applyFraction() {
                                return 0.95f;
                            }

                            @Override
                            protected void onStart(float x, float y) {
                                mItemList.touchHandling(false);
                                ac_content.showWithoutAnimation();
                            }

                            @Override
                            protected void onProgress(float x, float y, float slideValue, float fraction) {
                                content.setTranslationX(mDashWeight/2f * fraction/2f);
                            }

                            @Override
                            protected void onApply(float x, float y, float slideValue, float fraction) {
                                mItemList.touchHandling(true);
                                if(mPlayFilePointer == mFilePointer){
                                   ac_content.show();
                                   return;
                                }
                                ac_content.hideAndCustomize(new AppearanceController.AnimatorCustomization() {
                                    @Override
                                    public void customize(Animator animator) {
                                        final  FilePointer pointerToDelete = mFilePointer;
                                        animator.addListener(new AnimatorListenerSupport(){
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                if (!onSongDelete(pointerToDelete) && mFilePointer == pointerToDelete){
                                                    ac_content.show();
                                                }
                                            }
                                        });
                                    }
                                });

                            }

                            @Override
                            protected void onCancel(float x, float y, float slideValue, float fraction) {
                                mItemList.touchHandling(true);
                                ac_content.show();
                            }

                        });

                        if (position != -2) separator.setVisibility(View.GONE);
                        caption.setText(FormatUtils.getSongTitle(filePointer, getResources()));
                        description.setText(FormatUtils.getArtistString(filePointer, getResources()));
                        int color = 0;
                        int coverImageResource = R.drawable.android_note_lightgray;
                        float coverAlpha = 1f;
                        color = getResources().getColor(R.color.text_dark);
                        if (filePointer == mPlayFilePointer) {
                            if (mSongBuffering) {
                                coverAlpha = 0.4f;
                            }
                            if (mSongPlaying) {
                                coverImageResource = R.drawable.android_equ_pink;
                            } else {
                                coverImageResource = R.drawable.android_equ_lightgray;
                            }
                            color = getResources().getColor(R.color.text_highlight);
                        }
                        imageView.setAlpha(coverAlpha);
                        imageView.setImageResource(coverImageResource);
                        caption.setTextColor(color);
                        imageView.setFocusable(false);
                        imageView.setFocusableInTouchMode(false);

                    }

                    @Override
                    public void cleanup() {
                        separator.setVisibility(View.VISIBLE);
                    }
                };
            }
        }, R.layout.item_playlist);
        mItemList.setAdapter(mPlaylistAdapter);
        mItemList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onSongClick(mPlaylistAdapter.getItem(position));
            }
        });
    }

    private void action_onPlaylistSaveExisting() {
        mSavePlalistActionButton.setVisibility(View.GONE);
        if (mPlaylist.isSaveRequired()){
            application().function_playlistSave(mPlaylist, activity().observe(new ActivitySupport.OnValue<Void>() {
                @Override
                public void action(Void aVoid) {
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(), "Playlist saved", Toast.LENGTH_SHORT).show();
                    }
                }
            }));
        }
    }

    private void action_onPlaylistSave() {
        String title = view_text(R.id.edit_playlist_title).getText().toString();
        if (title.trim().isEmpty()){
            Toast.makeText(getActivity(),"Please set title",Toast.LENGTH_LONG).show();
            return;
        }
        mPlaylist.title=  title;
        mPlaylist.autosave = view_check(R.id.check_playlist_autosave).isChecked();
        application().function_playlistSave(mPlaylist, activity().observe(new ActivitySupport.OnValue<Void>() {
            @Override
            public void action(Void o) {
                if (activity() == null) return;
                update_playlist(mPlaylist);
                hide_playlistDetails();
            }
        }));
    }

    private void action_onPlaylistMore() {
        if (mPlaylist == null || mPlaylist.songList.isEmpty()){
            Toast.makeText(activity(), "Playlist should be not empty",Toast.LENGTH_LONG).show();
            return;
        }
        if (mPlaylist.title != null) {
            view_text(R.id.edit_playlist_title).setText(mPlaylist.title);
            view_check(R.id.check_playlist_autosave).setChecked(mPlaylist.autosave);
        }else {
            view_text(R.id.edit_playlist_title).setText("");
            view_check(R.id.check_playlist_autosave).setChecked(true);
        }
        if (mPlaylistDetailsShown){
            hide_playlistDetails();
        }else {
            update_playlistDetails();
        }
    }

    private void hide_playlistDetails() {
        ac_playlistDetails.hide();
        mPlaylistDetailsShown = false;
    }

    private void update_playlistDetails() {
        mPlaylistDetailsShown = true;
        ac_playlistDetails.show();

    }

    private boolean onSongDelete(FilePointer filePointer) {
        return application().player().playlist_removeFrom(mPlaylist, filePointer);
    }

    private void onSongClick(FilePointer filePointer) {
        if (mPlayFilePointer == filePointer){
            if (application().player().isSongPlaying()){
                application().player().pause();
            }else {
                application().player().resume();
            }
        }else {
            application().player().play(filePointer);
        }
    }

    private void hide_all() {
        hide_playlistDetails();
        mSavePlalistActionButton.setVisibility(View.GONE);
        mLoadingPanel.setVisibility(View.GONE);
        mPlayListPanel.setVisibility(View.GONE);
        mNoItemsPanel.setVisibility(View.GONE);
    }

    @Override
    public void onStart() {
        super.onStart();
        application().player().addPlayerListener(this);
        mPlayFilePointer = application().player().getCurrentSong();
        mSongPlaying = application().player().isSongPlaying();
        mSongBuffering = application().player().isBuffering();
        update_playlist(application().player().playlist());
        application().observers_songDetails.add(this);
        application().observers_playlistSave.add(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        application().player().removePlayerListener(this);
        application().observers_songDetails.remove(this);
        application().observers_playlistSave.remove(this);
    }

    @Override
    public void onPlaylistCalculation() {
        hide_all();
        mLoadingPanel.setVisibility(View.VISIBLE);
        mPlaylistText.setText("Playlist calculation");
        mSongsText.setText("Please wait");
    }

    @Override
    public void onPlaylistChanged(Playlist playlist) {
        update_playlist(playlist);
    }

    private void update_playlist(Playlist playlist) {
        if (mSwapInProgress) throw new IllegalStateException("Swap in progress");
        mPlaylist = playlist;
        hide_all();
        update_playlistSaveUI();
        if (playlist != null){
            if (playlist.title == null){
                mPlaylistText.setText("Recently Created");
            }else {
                mPlaylistText.setText(playlist.title);
            }
        }else {
            mPlaylistText.setText("Nothing to play");
        }
        if (playlist == null || playlist.songList.isEmpty()){
            mSongsText.setText("Add song to playlist");
        }

        if (playlist == null || playlist.songList.isEmpty()) {
            mNoItemsPanel.setVisibility(View.VISIBLE);
        } else {
            mSongsText.setText(playlist.songList.size()+" songs");
            mPlaylistAdapter.clear();
            mPlaylistAdapter.addAll(playlist.songList);
            update_plalistListView();
            mPlayListPanel.setVisibility(View.VISIBLE);
        }
    }

    private void update_playlistSaveUI() {
        if (mPlaylist != null) mSavePlalistActionButton.setVisibility(mPlaylist.isSaveRequired()? View.VISIBLE : View.GONE);
    }

    @Override
    public void onError(Throwable e) {
        activity().handle_Error(e);
    }

    @Override
    public void onUnavailableFile(FilePointer filePointer) {

    }

    @Override
    public void onCurrentSongChanged(FilePointer filePointer) {
        mPlayFilePointer = filePointer;
        mSongPlaying = application().player().isSongPlaying();
        mSongBuffering = true;
        update_plalistListView();
    }

    private void update_plalistListView() {
        if (mSwapInProgress) return;
        mPlaylistAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCurrentSongReady(FilePointer filePointer) {
        mPlayFilePointer = filePointer;
        mSongBuffering = false;
        update_plalistListView();
    }

    @Override
    public void onCurrentSongPlay() {
        mSongPlaying = true;
        update_plalistListView();
    }

    @Override
    public void onCurrentSongStop() {
        mSongPlaying = false;
        update_plalistListView();
    }

    @Override
    public void onCurrentSongSeekCompleted() {

    }

    @Override
    public void onDetails(FilePointer pointer, SongDetails songDetails) {
        update_plalistListView();
    }

    @Override
    public void onSwapStart() {
        mSwapInProgress = true;
    }

    @Override
    public void onSwapCanceled() {
        mSwapInProgress = false;
    }

    @Override
    public void onSwapFinished() {
        mSwapInProgress = false;
        List<FilePointer> fileList = new ArrayList<>();
        for (int i=0; i < mPlaylistAdapter.getCount(); i++ ){
            fileList.add(mPlaylistAdapter.getItem(i));
        }
        application().player().playlist_updateOrder(mPlaylist, fileList);
    }

    @Override
    public void onSave(Playlist playlist) {
        if (mPlaylist == playlist){
            update_playlistSaveUI();
        }
    }

    @Override
    public void onSaveRequired(Playlist playlist) {
        if (mPlaylist == playlist){
            update_playlistSaveUI();
        }
    }

    public static class PlaylistAdapter extends GenericListViewAdapter<FilePointer, GetViewImplementation.ViewHolder<FilePointer>> implements DynamicListAdapter {

        public PlaylistAdapter(Context context, GetViewImplementation.ViewHolderFactory<GetViewImplementation.ViewHolder<FilePointer>> factory, int layoutId) {
            super(context, factory, layoutId);
        }

        public PlaylistAdapter(Context context, int layoutId, GetViewImplementation<FilePointer, GetViewImplementation.ViewHolder<FilePointer>> getView) {
            super(context, layoutId, getView);
        }

        @Override
        public long getItemId(int position) {
            if (position < 0) return  -1;
            if (position>getCount()-1) return -1;
            return getItem(position).getSongId().hashCode();
        }

        @Override
        public void swapData(final int indexOne, final int indexTwo) {
            updateWithoutNotification(new Closure<GenericListViewAdapter<FilePointer, GetViewImplementation.ViewHolder<FilePointer>>, Void>() {
                @Override
                public Void execute(GenericListViewAdapter<FilePointer, GetViewImplementation.ViewHolder<FilePointer>> arg) {
                    int minPosition = Math.min(indexOne, indexTwo);
                    int maxPosition = Math.max(indexOne, indexTwo);
                    FilePointer minPositionData = getItem(minPosition);
                    FilePointer maxPositionData = getItem(maxPosition);
                    arg.remove(maxPositionData);
                    arg.remove(minPositionData);
                    arg.insert(maxPositionData, minPosition);
                    arg.insert(minPositionData, maxPosition);
                    return null;
                }
            });
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }

}
