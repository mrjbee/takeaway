package team.monroe.org.takeaway;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import org.monroe.team.android.box.app.ActivitySupport;

import team.monroe.org.takeaway.manage.SourceConfigurationManager;

public class ActivityConfiguration extends ActivitySupport<App>{
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

        SourceConfigurationManager.Configuration configuration = application().getSourceConfiguration();
        if (configuration != null){
            view_text(R.id.text_host).setText(configuration.host);
            view_text(R.id.text_port).setText(""+configuration.port);
            view_text(R.id.text_password).setText(configuration.password);
            view_text(R.id.text_user).setText(configuration.user);
        }
    }

    private void cancelConfigurationTest() {
        Toast.makeText(getApplicationContext(), "Configuration not saved", Toast.LENGTH_SHORT).show();
    }

    private void saveConfiguration() {
        final SourceConfigurationManager.Configuration configuration = new SourceConfigurationManager.Configuration(
                readText(R.id.text_host),
                readPortInt(R.id.text_port, 80),
                readText(R.id.text_user),
                readText(R.id.text_password)
        );

        if (configuration.host.isEmpty()){
            Toast.makeText(getApplicationContext(), "Host should be not empty", Toast.LENGTH_LONG).show();
            return;
        }
        mTestConnectionDialog.show();

        runLastOnUiThread(new Runnable() {
            @Override
            public void run() {
                onTestConnectionDone(configuration,true);
            }
        }, 5000);
    }

    private int readPortInt(int text_port, int i) {
       String portText = view_text(text_port).getText().toString().trim();
       if (portText.isEmpty()){
           return i;
       }
       return Integer.parseInt(portText);
    }

    private String readText(int textViewId) {
        return view_text(textViewId).getText().toString();
    }

    private void onTestConnectionDone(SourceConfigurationManager.Configuration configuration, boolean result) {
        if (mTestConnectionDialog == null) return;
        mTestConnectionDialog.dismiss();
        if (!result){
            Toast.makeText(getApplicationContext(), "Test failed", Toast.LENGTH_SHORT).show();
        }else{
            application().updateConfiguration(configuration);
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