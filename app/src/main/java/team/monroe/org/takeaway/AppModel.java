package team.monroe.org.takeaway;

import android.content.Context;

import org.monroe.team.android.box.app.AndroidModel;
import org.monroe.team.corebox.services.ServiceRegistry;

import team.monroe.org.takeaway.manage.SourceConfigurationManager;


public class AppModel extends AndroidModel {

    public AppModel(String appName, Context context) {
        super(appName, context);
    }

    @Override
    protected void constructor(String appName, Context context, ServiceRegistry serviceRegistry) {
        super.constructor(appName, context, serviceRegistry);
        serviceRegistry.registrate(SourceConfigurationManager.class, new SourceConfigurationManager(context));
    }
}
