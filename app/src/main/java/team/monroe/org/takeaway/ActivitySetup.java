package team.monroe.org.takeaway;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import org.monroe.team.android.box.app.ActivitySupport;

public class ActivitySetup extends ActivitySupport<App>{
    private ProgressDialog mTestConnectionDialog;

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
        mTestConnectionDialog = new ProgressDialog(this);
        mTestConnectionDialog.setTitle("Configuration Test");
        mTestConnectionDialog.setIndeterminate(true);
        mTestConnectionDialog.setMessage("Testing connection...");
        mTestConnectionDialog.setCancelable(true);
        mTestConnectionDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                cancelConfigurationTest();
            }
        });
    }

    private void cancelConfigurationTest() {
        Toast.makeText(getApplicationContext(), "Configuration not saved", Toast.LENGTH_SHORT).show();
    }

    private void saveConfiguration() {
        mTestConnectionDialog.show();
        runLastOnUiThread(new Runnable() {
            @Override
            public void run() {
                onTestConnectionDone(true);
            }
        }, 5000);
    }

    private void onTestConnectionDone(boolean result) {
        if (mTestConnectionDialog == null) return;
        mTestConnectionDialog.dismiss();
        if (!result){
            Toast.makeText(getApplicationContext(), "Test failed", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(getApplicationContext(), "Configuration saved", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTestConnectionDialog.dismiss();
        mTestConnectionDialog = null;
    }
}
