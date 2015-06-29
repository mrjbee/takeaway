package team.monroe.org.takeaway.fragment;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import org.monroe.team.android.box.app.ActivitySupport;
import org.monroe.team.android.box.app.ui.GenericListViewAdapter;
import org.monroe.team.android.box.app.ui.GetViewImplementation;
import org.monroe.team.android.box.data.Data;

import java.util.List;

import team.monroe.org.takeaway.R;
import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.Playlist;
import team.monroe.org.takeaway.presentations.Source;

public class FragmentDashboardPlayer extends FragmentDashboardActivity {

    private Data.DataChangeObserver<List<Source>> mPlaylistDataObserver;
    private View mLoadingPanel;
    private ListView mItemList;
    private View mNoItemsPanel;
    private Data.DataChangeObserver<Playlist> playlistDataChangeObserver;
    private GenericListViewAdapter<FilePointer, GetViewImplementation.ViewHolder<FilePointer>> mPlaylistAdapter;

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

        mLoadingPanel = view(R.id.panel_loading);
        mItemList = view_list(R.id.list_items);
        mNoItemsPanel = view(R.id.panel_no_items);
        hide_all();

        mPlaylistAdapter = new GenericListViewAdapter<FilePointer, GetViewImplementation.ViewHolder<FilePointer>>(activity(), new GetViewImplementation.ViewHolderFactory<GetViewImplementation.ViewHolder<FilePointer>>() {
            @Override
            public GetViewImplementation.ViewHolder<FilePointer> create(final View convertView) {
                return new GetViewImplementation.GenericViewHolder<FilePointer>() {

                    TextView caption = (TextView) convertView.findViewById(R.id.item_caption);

                    @Override
                    public void update(FilePointer filePointer, int position) {
                        caption.setText(filePointer.getNormalizedTitle());
                    }

                };
            }
        }, R.layout.item_debug);
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
        playlistDataChangeObserver = new Data.DataChangeObserver<Playlist>() {
            @Override
            public void onDataInvalid() {
                fetchPlaylist();
            }

            @Override
            public void onData(Playlist playlist) {

            }
        };
        application().player().data_active_playlist.addDataChangeObserver(playlistDataChangeObserver);
        fetchPlaylist();
    }

    @Override
    public void onStop() {
        super.onStop();
        application().player().data_active_playlist.removeDataChangeObserver(playlistDataChangeObserver);
    }

    private void fetchPlaylist() {
        hide_all();
        mLoadingPanel.setVisibility(View.VISIBLE);
        application().player().data_active_playlist.fetch(true, activity().observe_data(new ActivitySupport.OnValue<Playlist>() {
            @Override
            public void action(Playlist playlist) {
                hide_all();
                if (playlist == null || playlist.songList.isEmpty()) {
                    mNoItemsPanel.setVisibility(View.VISIBLE);
                } else {
                    //TODO: add more
                    mPlaylistAdapter.clear();
                    mPlaylistAdapter.addAll(playlist.songList);
                    mPlaylistAdapter.notifyDataSetChanged();
                    mItemList.setVisibility(View.VISIBLE);
                }
            }
        }));
    }
}
