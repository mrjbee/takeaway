package team.monroe.org.takeaway.fragment;

import android.os.Bundle;
import android.view.View;

import java.util.Arrays;
import java.util.List;

import team.monroe.org.takeaway.R;
import team.monroe.org.takeaway.view.HeaderItemViewPresenter;

public class FragmentDashboardHeader extends FragmentDashboardScreen {

    private HeaderItemViewPresenter.DefaultItemViewPresenter myMusicHeaderItem;
    private HeaderItemViewPresenter.DefaultItemViewPresenter searchHeaderItem;
    private HeaderItemViewPresenter.RootItemViewPresenter homeHeaderItem;
    private HeaderItemViewPresenter mCurrentPresenter;
    private List<HeaderItemViewPresenter> mHeaderItems;
    private int mCurrentPosition;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_dashboard_header;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        myMusicHeaderItem = new HeaderItemViewPresenter.DefaultItemViewPresenter(view(R.id.item_music),getActivity());
        searchHeaderItem = new HeaderItemViewPresenter.DefaultItemViewPresenter(view(R.id.item_search),getActivity());
        homeHeaderItem = new HeaderItemViewPresenter.RootItemViewPresenter(view(R.id.item_home));
        myMusicHeaderItem.setup("My Music", R.drawable.music,R.drawable.music_colored);
        searchHeaderItem.setup("Search", R.drawable.search,R.drawable.search_colored);
        myMusicHeaderItem.deselect(false);
        searchHeaderItem.deselect(false);
        homeHeaderItem.deselect(false);

        mCurrentPosition = getArguments().getInt("curr_position", 1);
        mHeaderItems = Arrays.asList(myMusicHeaderItem,homeHeaderItem,searchHeaderItem);
        select(mHeaderItems.get(mCurrentPosition), false);

        for (int i=0;i<mHeaderItems.size();i++) {
            final HeaderItemViewPresenter presenter = mHeaderItems.get(i);
            final int finalI = i;
            presenter.onClick(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    select(presenter, false);
                    dashboard().changeScreen(finalI);
                }
            });
        }

        view(R.id.panel_source).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dashboard().showSourcePopup(v);
            }
        });

    }

    public void select(int position) {
        if (mHeaderItems == null){
            getArguments().putInt("curr_position", position);
            return;
        }
        select(mHeaderItems.get(position), true);
    }

    private void select(HeaderItemViewPresenter presenter, boolean animate) {
        if (presenter == mCurrentPresenter){
           return;
        }
        presenter.select(animate);
        if (mCurrentPresenter != null) {
            mCurrentPresenter.deselect(animate);
        }
        mCurrentPresenter = presenter;
        mCurrentPosition = mHeaderItems.indexOf(mCurrentPresenter);
        getArguments().putInt("curr_position", mCurrentPosition);
    }
}
