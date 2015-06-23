package team.monroe.org.takeaway.fragment;

import android.animation.Animator;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import org.monroe.team.android.box.app.ui.animation.AnimatorListenerSupport;
import org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceController;
import org.monroe.team.android.box.utils.DisplayUtils;

import static org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceControllerBuilder.*;

import java.util.Arrays;
import java.util.List;

import team.monroe.org.takeaway.ActivityDashboard;
import team.monroe.org.takeaway.R;
import team.monroe.org.takeaway.view.HeaderItemViewPresenter;

public class FragmentDashboardHeader extends FragmentDashboardActivity {

    private HeaderItemViewPresenter.DefaultItemViewPresenter myMusicHeaderItem;
    private HeaderItemViewPresenter.DefaultItemViewPresenter searchHeaderItem;
    private HeaderItemViewPresenter.RootItemViewPresenter homeHeaderItem;
    private HeaderItemViewPresenter mCurrentPresenter;
    private List<HeaderItemViewPresenter> mHeaderItems;
    private int mCurrentPosition;
    private ViewGroup mSecondaryHeader;
    private AppearanceController ac_secondaryPanel;
    private boolean mSecondaryHeaderVisible = false;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_dashboard_header;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSecondaryHeader = view(R.id.panel_secondary_header, ViewGroup.class);

        ac_secondaryPanel = animateAppearance(mSecondaryHeader, heightSlide((int) DisplayUtils.dpToPx(50, getResources()),0))
                .showAnimation(duration_constant(500), interpreter_overshot())
                .hideAnimation(duration_constant(300), interpreter_accelerate(0.9f))
                .hideAndGone()
                .build();
        ac_secondaryPanel.hideWithoutAnimation();

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
        dashboard().subscribeSecondaryHeaderRequest(new ActivityDashboard.OnSecondaryHeaderRequestSubscriber() {
            @Override
            public void onRequest(View secondaryHeaderContent) {
                updateSecondaryHeader(secondaryHeaderContent);
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

    private void updateSecondaryHeader(final View view) {
        if (view == null){
            if (!mSecondaryHeaderVisible) return;
            mSecondaryHeaderVisible = false;
            ac_secondaryPanel.hideAndCustomize(new AppearanceController.AnimatorCustomization() {
                @Override
                public void customize(Animator animator) {
                    animator.addListener(new AnimatorListenerSupport(){
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mSecondaryHeader.removeAllViews();
                        }
                    });
                }
            });
        }else {
            if (mSecondaryHeaderVisible){
                mSecondaryHeader.removeAllViews();
                mSecondaryHeader.addView(view);
            }else {
                mSecondaryHeaderVisible = true;
                ac_secondaryPanel.showAndCustomize(new AppearanceController.AnimatorCustomization() {
                    @Override
                    public void customize(Animator animator) {
                        animator.addListener(new AnimatorListenerSupport() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                mSecondaryHeader.removeAllViews();
                                mSecondaryHeader.addView(view);
                            }
                        });
                    }
                });
            }
        }
    }
}
