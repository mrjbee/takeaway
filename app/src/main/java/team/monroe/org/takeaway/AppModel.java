package team.monroe.org.takeaway;

import android.content.Context;

import org.monroe.team.android.box.app.AndroidModel;
import org.monroe.team.android.box.services.HttpManager;
import org.monroe.team.android.box.services.NetworkManager;
import org.monroe.team.corebox.services.ServiceRegistry;

import team.monroe.org.takeaway.manage.CloudConfigurationManager;
import team.monroe.org.takeaway.manage.CloudConnectionManager;
import team.monroe.org.takeaway.manage.CloudManager;
import team.monroe.org.takeaway.manage.FileProvider;
import team.monroe.org.takeaway.manage.KodiFileProvider;
import team.monroe.org.takeaway.manage.impl.KodiCloudManager;


public class AppModel extends AndroidModel {

    public AppModel(String appName, Context context) {
        super(appName, context);
    }

    @Override
    protected void constructor(String appName, Context context, ServiceRegistry serviceRegistry) {
        super.constructor(appName, context, serviceRegistry);

        serviceRegistry.registrate(NetworkManager.class, new NetworkManager(context));
        CloudConnectionManager cloudConnectionManager = new CloudConnectionManager(this);
        serviceRegistry.registrate(CloudConnectionManager.class, cloudConnectionManager);
        CloudConfigurationManager configurationManager = new CloudConfigurationManager(context);
        serviceRegistry.registrate(CloudConfigurationManager.class, configurationManager);
        CloudManager cloudManager = new KodiCloudManager(new HttpManager());
        FileProvider fileProvider = new KodiFileProvider(cloudManager, cloudConnectionManager, configurationManager);
        serviceRegistry.registrate(CloudManager.class, cloudManager);
        serviceRegistry.registrate(FileProvider.class, fileProvider);
    }
}
