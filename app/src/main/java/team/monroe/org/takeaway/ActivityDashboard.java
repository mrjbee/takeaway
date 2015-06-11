package team.monroe.org.takeaway;

import android.app.Fragment;
import android.os.Bundle;

import org.monroe.team.android.box.app.ActivitySupport;

import team.monroe.org.takeaway.fragment.FragmentDashboardHeader;
import team.monroe.org.takeaway.fragment.FragmentDashboardScreensPager;
import team.monroe.org.takeaway.fragment.contract.ContractBackButton;


public class ActivityDashboard extends ActivitySupport<App>{

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
}
