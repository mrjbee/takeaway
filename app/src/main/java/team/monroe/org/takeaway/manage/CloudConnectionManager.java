package team.monroe.org.takeaway.manage;

import android.content.Context;

import org.monroe.team.android.box.services.NetworkManager;
import org.monroe.team.corebox.app.Model;

import java.util.Timer;
import java.util.TimerTask;

import team.monroe.org.takeaway.presentations.SourceConnectionStatus;
import team.monroe.org.takeaway.uc.CheckCloudConnection;

public class CloudConnectionManager {

    private final Model model;
    private ConnectionStatus mStatus = ConnectionStatus.NOT_DEFINED;
    private Timer mTimer;

    public CloudConnectionManager(Model model) {
        this.model = model;
    }

    public ConnectionStatus getStatus(){
        return mStatus;
    }

    public synchronized void startWatcher() {
        stopWatcher();
        mTimer = new Timer("cloud_connection_watcher",true);
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                checkAndSchedule();
            }
        }, getDelayForNextCheck());
    }

    public synchronized void stopWatcher() {
        if (mTimer != null){
            mTimer.cancel();
            mTimer.purge();
        }
    }

    private void checkAndSchedule() {
        CloudConfigurationManager.Configuration configuration = model.usingService(CloudConfigurationManager.class).get();
        if (configuration == null){
            updateStatus(ConnectionStatus.NOT_CONFIGURED);
        }else {
            SourceConnectionStatus mAnswer = model.execute(CheckCloudConnection.class, configuration);
            updateStatusBySourceConnection(mAnswer, true);
        }
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                checkAndSchedule();
            }
        }, getDelayForNextCheck());
    }

    private int getDelayForNextCheck() {
        boolean wifiEnabled = model.usingService(NetworkManager.class).isUsingWifi();
        if (mStatus == ConnectionStatus.ONLINE){
            return 10000;
        }else if (wifiEnabled){
            return 2000;
        } else if (mStatus == ConnectionStatus.NOT_DEFINED){
            return 0;
        }else {
            return 5000;
        }
    }

    public void updateStatusBySourceConnection(SourceConnectionStatus answer) {
        updateStatusBySourceConnection(answer, false);
    }

    private void updateStatusBySourceConnection(SourceConnectionStatus answer, boolean internalUse) {
        if (answer.status == CloudManager.Status.BAD_CONNECTION
            || answer.status == CloudManager.Status.BAD_URL
            || answer.status == CloudManager.Status.NO_ROUTE_TO_HOST){
                updateStatus(ConnectionStatus.OFFLINE);
                if (!internalUse){
                    //Because we want to reschedule timer
                    startWatcher();
                }
        }else {
            updateStatus(ConnectionStatus.ONLINE);
        }
    }

    private void updateStatus(ConnectionStatus status) {
        mStatus = status;
        Events.CLOUD_CONNECTION_STATUS.send(model.usingService(Context.class), status);
    }


    public static enum ConnectionStatus{
        NOT_DEFINED, NOT_CONFIGURED, ONLINE, OFFLINE
    }
}
