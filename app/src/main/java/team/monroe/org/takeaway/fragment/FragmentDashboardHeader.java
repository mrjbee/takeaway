package team.monroe.org.takeaway.fragment;

import android.animation.Animator;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import org.monroe.team.android.box.app.ui.animation.AnimatorListenerSupport;
import org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceController;
import org.monroe.team.android.box.event.Event;
import org.monroe.team.android.box.utils.DisplayUtils;
import org.monroe.team.corebox.utils.Closure;

import static org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceControllerBuilder.*;

import java.util.Arrays;
import java.util.List;

import team.monroe.org.takeaway.ActivityDashboard;
import team.monroe.org.takeaway.R;
import team.monroe.org.takeaway.manage.CloudConnectionManager;
import team.monroe.org.takeaway.manage.Events;
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
    private ImageButton mCloudAction;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_dashboard_header;
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
        mCloudAction = view(R.id.action_cloud, ImageButton.class);

        mCloudAction.setOnClickListener(new View.OnClickListener() {
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

        updateConnectionStatus(application().getConnectionStatus());
    }

    @Override
    public void onStart() {
        super.onStart();
        Event.subscribeOnEvent(activity(), this, Events.CLOUD_CONNECTION_STATUS, new Closure<CloudConnectionManager.ConnectionStatus, Void>() {
            @Override
            public Void execute(CloudConnectionManager.ConnectionStatus status) {
                updateConnectionStatus(status);
                return null;
            }
        });
    }

    private void updateConnectionStatus(CloudConnectionManager.ConnectionStatus status) {
        int iconResource;
        switch (status){
            case ONLINE:
                iconResource = R.drawable.android_cloud;
                break;
            case OFFLINE:
                iconResource = R.drawable.android_cloud_off;
                break;
            default:
                iconResource = R.drawable.android_cloud_not_defined;
                break;
        }
        mCloudAction.setImageResource(iconResource);
    }

    @Override
    public void onStop() {
        super.onStop();
        Event.unSubscribeFromEvents(activity(), this);
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
