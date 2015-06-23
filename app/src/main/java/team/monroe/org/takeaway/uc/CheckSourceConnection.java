package team.monroe.org.takeaway.uc;

import org.monroe.team.corebox.services.ServiceRegistry;
import org.monroe.team.corebox.uc.UserCaseSupport;

import team.monroe.org.takeaway.manage.CloudConfigurationManager;
import team.monroe.org.takeaway.manage.CloudManager;
import team.monroe.org.takeaway.presentations.SourceConnectionStatus;

public class CheckSourceConnection extends UserCaseSupport<CloudConfigurationManager.Configuration, SourceConnectionStatus>{

    public CheckSourceConnection(ServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }

    @Override
    protected SourceConnectionStatus executeImpl(CloudConfigurationManager.Configuration request) {
        CloudManager.Answer<String> versionAnswer = using(CloudManager.class).getSourceVersion(request);
        SourceConnectionStatus connectionStatus = SourceConnectionStatus.fromAnswer(versionAnswer);
        if (connectionStatus.isSuccess()){
            using(CloudConfigurationManager.class).update(request);
            using(CloudConfigurationManager.class).putProperty("version", versionAnswer.body);
        }

        return connectionStatus;
    }

}
