package team.monroe.org.takeaway.fragment;

import android.os.Bundle;
import android.view.View;

import org.monroe.team.android.box.app.FragmentSupport;

import team.monroe.org.takeaway.ActivityDashboard;
import team.monroe.org.takeaway.App;
import team.monroe.org.takeaway.R;

public abstract class FragmentDashboardSlide extends FragmentSupport<App> {

    private View mSecondaryHeaderView;

    public void onSelect() {
        dashboard().requestSecondaryHeader(mSecondaryHeaderView);
    }

    public void requestSecondaryHeader(View view){
        mSecondaryHeaderView = view;
        if (dashboard().isSlideSelected(this)){
            dashboard().requestSecondaryHeader(mSecondaryHeaderView);
        }
    }

    public ActivityDashboard dashboard(){
        return (ActivityDashboard) activity();
    }
}
