package team.monroe.org.takeaway;

import android.app.Dialog;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupWindow;

import org.monroe.team.android.box.app.ActivitySupport;
import org.monroe.team.android.box.utils.DisplayUtils;

import team.monroe.org.takeaway.fragment.FragmentDashboardHeader;
import team.monroe.org.takeaway.fragment.FragmentDashboardScreensPager;
import team.monroe.org.takeaway.fragment.contract.ContractBackButton;


public class ActivityDashboard extends ActivitySupport<App>{

    private PopupWindow mNoSourcePopup;
    private PopupWindow mSourcePopup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (isFirstRun(savedInstanceState)){
            FragmentDashboardScreensPager fragmentDashboardScreensPager = new FragmentDashboardScreensPager();
            Bundle bundle = new Bundle();
            bundle.putInt("curr_position", 1);
            bundle.putBoolean("first_run", true);
            FragmentDashboardHeader dashboardHeader = new FragmentDashboardHeader();
            dashboardHeader.setArguments(bundle);
            fragmentDashboardScreensPager.setArguments(bundle);
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.frag_body, fragmentDashboardScreensPager)
                    .add(R.id.frag_header, dashboardHeader)
                    .commit();
        }
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
        FragmentDashboardScreensPager fragment = (FragmentDashboardScreensPager) getFragmentManager().findFragmentById(R.id.frag_body);
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
}
