package team.monroe.org.takeaway.fragment;

import android.os.Bundle;
import android.view.View;

import org.monroe.team.android.box.event.Event;
import org.monroe.team.corebox.utils.Closure;

import team.monroe.org.takeaway.R;
import team.monroe.org.takeaway.manage.CloudConnectionManager;
import team.monroe.org.takeaway.manage.Events;

public class FragmentDownloads extends FragmentDashboardActivity{
    @Override
    protected int getLayoutId() {
        return R.layout.fragment_dashboard_downloads;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        view(R.id.action_setup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dashboard().requestConfigurationActivity(false);
            }
        });

        view(R.id.action_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dashboard().hideDownloads();
            }
        });

        view_text(R.id.text_cloud_title).setText(application().function_cloudName());
        updateCloudStatus(application().function_cloudConnectionStatus());
    }

    private void updateCloudStatus(CloudConnectionManager.ConnectionStatus connectionStatus) {
        String status = "Online";
        switch (connectionStatus){
            case OFFLINE:
                status = "Offline";
                break;
            case ONLINE:
                status = "Online";
                break;
            case NOT_CONFIGURED:
                status = "Not configured";
                break;
            case NOT_DEFINED:
                status = "Unknown";
                break;
            default:
                throw new IllegalStateException();
        }
        view_text(R.id.text_cloud_status).setText(status);
    }

    @Override
    public void onStart() {
        super.onStart();
        Event.subscribeOnEvent(activity(), this, Events.CLOUD_CONNECTION_STATUS, new Closure<CloudConnectionManager.ConnectionStatus, Void>() {
            @Override
            public Void execute(CloudConnectionManager.ConnectionStatus arg) {
                updateCloudStatus(arg);
                return null;
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        Event.unSubscribeFromEvents(activity(), this);
    }
}
