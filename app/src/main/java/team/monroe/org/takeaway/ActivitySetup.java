package team.monroe.org.takeaway;

import android.os.Bundle;
import android.view.View;

import org.monroe.team.android.box.app.ActivitySupport;

public class ActivitySetup extends ActivitySupport<App>{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure);
        view(R.id.action_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        view(R.id.action_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveConfiguration();
            }
        });
    }

    private void saveConfiguration() {

    }
}
