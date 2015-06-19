package team.monroe.org.takeaway;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.PopupWindow;

import org.monroe.team.android.box.app.ActivitySupport;
import org.monroe.team.android.box.utils.DisplayUtils;

import team.monroe.org.takeaway.fragment.FragmentDashboardHeader;
import team.monroe.org.takeaway.fragment.FragmentDashboardMediaSourceConfiguration;
import team.monroe.org.takeaway.fragment.FragmentDashboardPagerSlider;
import team.monroe.org.takeaway.fragment.contract.ContractBackButton;


public class ActivityDashboard extends ActivitySupport<App>{

    private static final int REQUEST_CONFIGURATION = 101;
    private PopupWindow mNoSourcePopup;
    private PopupWindow mSourcePopup;

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
        if (!application().isSourceConfigured()){
            if (mNoSourcePopup == null){
                View view = getLayoutInflater().inflate(R.layout.popup_source_not_set, null);
                view.findViewById(R.id.action_setup).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mNoSourcePopup.dismiss();
                        startActivity(new Intent(getApplicationContext(), ActivityConfiguration.class));
                    }
                });
                mNoSourcePopup = new PopupWindow(view,
                        (int) DisplayUtils.dpToPx(260, getResources()),
                        (int) DisplayUtils.dpToPx(120, getResources()),
                        true);

            }
            mNoSourcePopup.setOutsideTouchable(true);
            mNoSourcePopup.setFocusable(true);
            mNoSourcePopup.setBackgroundDrawable(getResources().getDrawable(R.color.transperent));
            mNoSourcePopup.showAsDropDown(anchor);
        }else {
            if (mSourcePopup == null){
                View view = getLayoutInflater().inflate(R.layout.popup_source, null);
                view.findViewById(R.id.action_setup).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSourcePopup.dismiss();
                        startActivity(new Intent(getApplicationContext(), ActivityConfiguration.class));
                    }
                });
                mSourcePopup = new PopupWindow(view,
                        (int) DisplayUtils.dpToPx(260, getResources()),
                        (int) DisplayUtils.dpToPx(120, getResources()),
                        true);

        }
        mSourcePopup.setOutsideTouchable(true);
        mSourcePopup.setFocusable(true);
        mSourcePopup.setBackgroundDrawable(getResources().getDrawable(R.color.transperent));
        mSourcePopup.showAsDropDown(anchor);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mNoSourcePopup != null){
            mNoSourcePopup.dismiss();
            mNoSourcePopup = null;
        }

        if (mSourcePopup != null){
            mSourcePopup.dismiss();
            mSourcePopup = null;
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

}
