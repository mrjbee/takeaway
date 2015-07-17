package team.monroe.org.takeaway.fragment;

import android.animation.Animator;
import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

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

public class FragmentDashboardDrawerPlaylist extends FragmentDashboardActivity implements Player.PlayerListener, App.OnSongDetailsObserver, DynamicListView.OnElementsSwapListener {

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
        hide_all();

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
    }

    @Override
    public void onStop() {
        super.onStop();
        application().player().removePlayerListener(this);
    }

    @Override
    public void onPlaylistCalculation() {
        hide_all();
        mLoadingPanel.setVisibility(View.VISIBLE);
        mSongsText.setText("Loading. Please wait");
    }

    @Override
    public void onPlaylistChanged(Playlist playlist) {
        update_playlist(playlist);
    }

    private void update_playlist(Playlist playlist) {
        if (mSwapInProgress) throw new IllegalStateException("Swap in progress");
        mPlaylist = playlist;
        hide_all();
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
