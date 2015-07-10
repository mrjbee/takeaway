package team.monroe.org.takeaway.fragment;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import org.monroe.team.android.box.app.ui.GenericListViewAdapter;
import org.monroe.team.android.box.app.ui.GetViewImplementation;
import org.monroe.team.android.box.data.Data;

import team.monroe.org.takeaway.R;
import team.monroe.org.takeaway.manage.Player;
import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.Playlist;

public class FragmentDashboardPlayer extends FragmentDashboardActivity implements Player.PlayerListener{

    private View mLoadingPanel;
    private ListView mItemList;
    private View mNoItemsPanel;
    private GenericListViewAdapter<FilePointer, GetViewImplementation.ViewHolder<FilePointer>> mPlaylistAdapter;
    private TextView mPlaylistText;
    private TextView mSongsText;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_drawer_player;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getFragmentView().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        View mSongsListHeaderView = getActivity().getLayoutInflater().inflate(R.layout.panel_player_top, null);
        mPlaylistText = (TextView) mSongsListHeaderView.findViewById(R.id.text_playlist_title);
        mSongsText = (TextView) mSongsListHeaderView.findViewById(R.id.text_song_count);

        mLoadingPanel = view(R.id.panel_loading);
        mItemList = view_list(R.id.list_items);
        mItemList.addHeaderView(mSongsListHeaderView, null, false);
        mNoItemsPanel = view(R.id.panel_no_items);
        hide_all();

        mPlaylistAdapter = new GenericListViewAdapter<FilePointer, GetViewImplementation.ViewHolder<FilePointer>>(activity(), new GetViewImplementation.ViewHolderFactory<GetViewImplementation.ViewHolder<FilePointer>>() {
            @Override
            public GetViewImplementation.ViewHolder<FilePointer> create(final View convertView) {
                return new GetViewImplementation.GenericViewHolder<FilePointer>() {

                    TextView caption = (TextView) convertView.findViewById(R.id.item_caption);
                    TextView description = (TextView) convertView.findViewById(R.id.item_description);
                    View separator = convertView.findViewById(R.id.separator);

                    @Override
                    public void update(FilePointer filePointer, int position) {
                        if (position == 0) separator.setVisibility(View.GONE);

                        caption.setText(filePointer.getNormalizedTitle());
                        description.setText(filePointer.relativePath);
                    }

                    @Override
                    public void cleanup() {
                        separator.setVisibility(View.VISIBLE);
                    }
                };
            }
        }, R.layout.item_playlist);
        mItemList.setAdapter(mPlaylistAdapter);
    }

    private void hide_all() {
        mLoadingPanel.setVisibility(View.GONE);
        mItemList.setVisibility(View.GONE);
        mNoItemsPanel.setVisibility(View.GONE);
    }


    @Override
    public void onStart() {
        super.onStart();
        application().player().addPlayerListener(this);
        update_playlist(application().player().getPlaylist());
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
    }

    @Override
    public void onPlaylistChanged(Playlist playlist) {
        update_playlist(playlist);
    }

    private void update_playlist(Playlist playlist) {
        hide_all();
        if (playlist == null || playlist.songList.isEmpty()) {
            mNoItemsPanel.setVisibility(View.VISIBLE);
        } else {

            mSongsText.setText(playlist.songList.size()+" songs");
            mPlaylistText.setText(playlist.title);

            mPlaylistAdapter.clear();
            mPlaylistAdapter.addAll(playlist.songList);
            mPlaylistAdapter.notifyDataSetChanged();

            mItemList.setVisibility(View.VISIBLE);
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

    }

    @Override
    public void onCurrentSongReady(FilePointer filePointer) {

    }

    @Override
    public void onCurrentSongPlay() {

    }

    @Override
    public void onCurrentSongStop() {

    }
}
