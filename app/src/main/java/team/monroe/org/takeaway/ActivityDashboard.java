package team.monroe.org.takeaway;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.PopupWindow;

import org.monroe.team.android.box.app.ActivitySupport;
import static org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceControllerBuilder.*;

import org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceController;
import org.monroe.team.android.box.utils.DisplayUtils;

import team.monroe.org.takeaway.fragment.FragmentDashboardHeader;
import team.monroe.org.takeaway.fragment.FragmentDashboardMediaSourceConfiguration;
import team.monroe.org.takeaway.fragment.FragmentDashboardPagerSlider;
import team.monroe.org.takeaway.fragment.FragmentDashboardSlide;
import team.monroe.org.takeaway.fragment.FragmentDownloads;
import team.monroe.org.takeaway.fragment.contract.ContractBackButton;


public class ActivityDashboard extends ActivitySupport<App>{

    private static final int REQUEST_CONFIGURATION = 101;
    private OnSecondaryHeaderRequestSubscriber mSecondaryHeaderSubscriber;
    private View mSecondaryHeaderRequestView;
    private AppearanceController ac_downloadFragment;
    private boolean mDownloadsShown =false;

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
            getFragmentManager().beginTransaction().add(R.id.frag_downloads,new FragmentDownloads()).commit();
        }

        view(R.id.layer_shadow).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        ac_downloadFragment = combine(
                animateAppearance(view(R.id.layer_shadow), alpha(0.8f,0f))
                        .showAnimation(duration_constant(400),interpreter_overshot())
                        .hideAnimation(duration_constant(200), interpreter_decelerate(0.5f))
                        .hideAndGone(),
                animateAppearance(view(R.id.frag_downloads), ySlide(0, DisplayUtils.screenHeight(getResources())))
                        .showAnimation(duration_constant(300), interpreter_accelerate(0.5f))
                        .hideAnimation(duration_constant(200), interpreter_decelerate(0.5f))
                        .hideAndGone()
        );

        if (savedInstanceState != null){
            mDownloadsShown = savedInstanceState.getBoolean("downloads_shown",false);
        }

        if (mDownloadsShown){
            ac_downloadFragment.showWithoutAnimation();
        }else {
            ac_downloadFragment.hideWithoutAnimation();
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
        }},500);
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
        if (mDownloadsShown){
            hideDownloads();
            return;
        }
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

    public void showDownloads() {
        mDownloadsShown = true;
        ac_downloadFragment.show();
    }

    public void hideDownloads() {
        mDownloadsShown = false;
        ac_downloadFragment.hide();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("downloads_shown", mDownloadsShown);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void requestConfigurationActivity(boolean transformUI) {
        if (transformUI) {
            startActivityForResult(new Intent(getApplicationContext(), ActivityConfiguration.class), REQUEST_CONFIGURATION);
        } else {
            startActivity(new Intent(getApplicationContext(), ActivityConfiguration.class));
        }
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
