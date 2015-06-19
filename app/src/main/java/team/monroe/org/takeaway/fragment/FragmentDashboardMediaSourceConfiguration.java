package team.monroe.org.takeaway.fragment;

import android.os.Bundle;
import android.view.View;

import team.monroe.org.takeaway.R;

public class FragmentDashboardMediaSourceConfiguration extends FragmentDashboardActivity{

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_dashboard_media_source_configuration;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        view(R.id.action_configure).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dashboard().requestConfigurationActivity();
            }
        });
    }
}
