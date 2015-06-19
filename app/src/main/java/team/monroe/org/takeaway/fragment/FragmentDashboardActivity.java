package team.monroe.org.takeaway.fragment;

import org.monroe.team.android.box.app.FragmentSupport;

import team.monroe.org.takeaway.ActivityDashboard;
import team.monroe.org.takeaway.App;
import team.monroe.org.takeaway.fragment.contract.ContractBackButton;

public abstract class FragmentDashboardActivity extends FragmentSupport<App> {

    public ActivityDashboard dashboard(){
        return (ActivityDashboard) activity();
    }
}
