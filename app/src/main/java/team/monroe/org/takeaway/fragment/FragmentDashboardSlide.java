package team.monroe.org.takeaway.fragment;

import android.os.Bundle;

import org.monroe.team.android.box.app.FragmentSupport;

import team.monroe.org.takeaway.App;
import team.monroe.org.takeaway.R;

public abstract class FragmentDashboardSlide extends FragmentSupport<App> {
    @Override
    protected int getLayoutId() {
        return R.layout.fragment_dashboard_slide;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        view_text(R.id.debug).setText(getHelloString());
    }

    protected abstract String getHelloString();

}
