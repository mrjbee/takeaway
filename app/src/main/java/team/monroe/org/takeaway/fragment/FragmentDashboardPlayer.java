package team.monroe.org.takeaway.fragment;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import team.monroe.org.takeaway.R;

public class FragmentDashboardPlayer extends FragmentDashboardActivity {
    @Override
    protected int getLayoutId() {
        return R.layout.fragment_drawer_player;
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
    }
}
