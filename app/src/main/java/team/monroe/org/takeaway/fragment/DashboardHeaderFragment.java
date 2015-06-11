package team.monroe.org.takeaway.fragment;

import android.os.Bundle;
import android.view.View;

import org.monroe.team.android.box.app.FragmentSupport;

import team.monroe.org.takeaway.App;
import team.monroe.org.takeaway.R;
import team.monroe.org.takeaway.view.HeaderItemViewPresenter;

public class DashboardHeaderFragment extends FragmentSupport<App> {

    private HeaderItemViewPresenter.DefaultItemViewPresenter myMusicHeaderItem;
    private HeaderItemViewPresenter.DefaultItemViewPresenter searchHeaderItem;
    private HeaderItemViewPresenter.RootItemViewPresenter homeHeaderItem;
    private HeaderItemViewPresenter mCurrentPresenter;

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
        homeHeaderItem.select(true);
        mCurrentPresenter = homeHeaderItem;
        HeaderItemViewPresenter[] presenters = {myMusicHeaderItem,homeHeaderItem,searchHeaderItem};
        for (final HeaderItemViewPresenter presenter : presenters) {
            presenter.onClick(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (presenter == mCurrentPresenter){
                       return;
                    }
                    presenter.select(true);
                    mCurrentPresenter.deselect(true);
                    mCurrentPresenter = presenter;
                }
            });
        }

    }
}
