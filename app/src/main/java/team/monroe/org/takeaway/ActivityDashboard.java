package team.monroe.org.takeaway;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.PopupWindow;

import org.monroe.team.android.box.app.ActivitySupport;
import org.monroe.team.android.box.utils.DisplayUtils;

import team.monroe.org.takeaway.fragment.FragmentDashboardHeader;
import team.monroe.org.takeaway.fragment.FragmentDashboardMediaSourceConfiguration;
import team.monroe.org.takeaway.fragment.FragmentDashboardPagerSlider;
import team.monroe.org.takeaway.fragment.FragmentDashboardSlide;
import team.monroe.org.takeaway.fragment.contract.ContractBackButton;


public class ActivityDashboard extends ActivitySupport<App>{

    private static final int REQUEST_CONFIGURATION = 101;
    private PopupWindow mCloudPopup;
    private OnSecondaryHeaderRequestSubscriber mSecondaryHeaderSubscriber;
    private View mSecondaryHeaderRequestView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (isFirstRun(savedInstanceState)){
            if (application().isSourceConfigured()){
                //usual look
                setup_onCreateSlider();
            }else{
               //intro
                setup_onCreateSourceConfiguration();
            }
        }
    }

    private void setup_onCreateSourceConfiguration() {
        FragmentDashboardMediaSourceConfiguration bodyFragment = new FragmentDashboardMediaSourceConfiguration();
        Bundle bundle = new Bundle();
        bodyFragment.setArguments(bundle);
        getFragmentManager()
                .beginTransaction()
                .add(R.id.frag_body, bodyFragment)
                .commit();
    }


    private void setup_Slider() {
        runLastOnUiThread(new Runnable() {
            @Override
            public void run() {
                FragmentDashboardPagerSlider fragmentDashboardPagerSlider = new FragmentDashboardPagerSlider();
                Bundle bundle = new Bundle();
                bundle.putInt("curr_position", 1);
                bundle.putBoolean("first_run", true);
                FragmentDashboardHeader dashboardHeader = new FragmentDashboardHeader();
                dashboardHeader.setArguments(bundle);
                fragmentDashboardPagerSlider.setArguments(bundle);
                view(R.id.frag_header).setTranslationY(-DisplayUtils.dpToPx(200,getResources()));

                getFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.animator.gone_up, R.animator.gone_down)
                        .replace(R.id.frag_body, fragmentDashboardPagerSlider)
                        .add(R.id.frag_header, dashboardHeader)
                        .commit();

                view(R.id.frag_header).animate().setStartDelay(800).setDuration(400).translationY(0).setInterpolator(new AccelerateDecelerateInterpolator()).start();
            }
        },500);
    }

    private void setup_onCreateSlider() {
        FragmentDashboardPagerSlider fragmentDashboardPagerSlider = new FragmentDashboardPagerSlider();
        Bundle bundle = new Bundle();
        bundle.putInt("curr_position", 1);
        bundle.putBoolean("first_run", true);
        FragmentDashboardHeader dashboardHeader = new FragmentDashboardHeader();
        dashboardHeader.setArguments(bundle);
        fragmentDashboardPagerSlider.setArguments(bundle);
        getFragmentManager()
                .beginTransaction()
                .add(R.id.frag_body, fragmentDashboardPagerSlider)
                .add(R.id.frag_header, dashboardHeader)
                .commit();
    }


    @Override
    public void onBackPressed() {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.frag_body);
        if (fragment instanceof ContractBackButton){
            if (((ContractBackButton) fragment).onBackPressed()){
                return;
            }
        }
        super.onBackPressed();
    }

    public void onScreenChanged(int position) {
        FragmentDashboardHeader header = (FragmentDashboardHeader) getFragmentManager().findFragmentById(R.id.frag_header);
        header.select(position);
    }

    public void changeScreen(int screenPosition) {
        FragmentDashboardPagerSlider fragment = (FragmentDashboardPagerSlider) getFragmentManager().findFragmentById(R.id.frag_body);
        fragment.updateScreen(screenPosition);
    }

    public void showSourcePopup(View anchor) {
        if (mCloudPopup == null) {
            View view = getLayoutInflater().inflate(R.layout.popup_cloud, null);
            view.findViewById(R.id.action_setup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCloudPopup.dismiss();
                startActivity(new Intent(getApplicationContext(), ActivityConfiguration.class));
            }
            });
            mCloudPopup = new PopupWindow(view,
                (int) DisplayUtils.dpToPx(300, getResources()),
                (int) DisplayUtils.dpToPx(150, getResources()),
                true);
        }
        mCloudPopup.setOutsideTouchable(true);
        mCloudPopup.setFocusable(true);
        mCloudPopup.setBackgroundDrawable(getResources().getDrawable(R.color.transperent));
        mCloudPopup.showAsDropDown(anchor,
                (int)DisplayUtils.dpToPx(-100, getResources()),
                (int)DisplayUtils.dpToPx(-10, getResources()));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mCloudPopup != null){
            mCloudPopup.dismiss();
            mCloudPopup = null;
        }
    }

    public void requestConfigurationActivity() {
        startActivityForResult(new Intent(getApplicationContext(), ActivityConfiguration.class), REQUEST_CONFIGURATION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CONFIGURATION && resultCode == Activity.RESULT_OK){
            setup_Slider();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void requestSecondaryHeader(View view) {
        mSecondaryHeaderRequestView = view;
        if (mSecondaryHeaderSubscriber != null){
            mSecondaryHeaderSubscriber.onRequest(mSecondaryHeaderRequestView);
        }
    }

    public void subscribeSecondaryHeaderRequest(OnSecondaryHeaderRequestSubscriber subscriber) {
        mSecondaryHeaderSubscriber = subscriber;
        if (mSecondaryHeaderRequestView != null){
            mSecondaryHeaderSubscriber.onRequest(mSecondaryHeaderRequestView);
        }
    }

    public boolean isSlideSelected(FragmentDashboardSlide slide) {
        Fragment bodyFragment = getFragmentManager().findFragmentById(R.id.frag_body);
        if (!(bodyFragment instanceof FragmentDashboardPagerSlider)){
            return false;
        }
        FragmentDashboardPagerSlider pagerSlider = (FragmentDashboardPagerSlider) bodyFragment;
        FragmentDashboardSlide dashboardSlide = pagerSlider.getCurrentSlide();
        return dashboardSlide == slide;
    }

    public static interface OnSecondaryHeaderRequestSubscriber {
        public void onRequest(View secondaryHeaderContent);
    }
}
