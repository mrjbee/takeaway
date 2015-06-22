package team.monroe.org.takeaway.uc;

import org.monroe.team.corebox.services.ServiceRegistry;
import org.monroe.team.corebox.uc.UserCaseSupport;

import team.monroe.org.takeaway.manage.SourceConfigurationManager;
import team.monroe.org.takeaway.manage.SourceManager;
import team.monroe.org.takeaway.presentations.SourceConnectionStatus;

public class CheckSourceConnection extends UserCaseSupport<SourceConfigurationManager.Configuration, SourceConnectionStatus>{

    public CheckSourceConnection(ServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }

    @Override
    protected SourceConnectionStatus executeImpl(SourceConfigurationManager.Configuration request) {
        SourceManager.Answer<String> versionAnswer = using(SourceManager.class).getSourceVersion(request);
        SourceConnectionStatus connectionStatus = SourceConnectionStatus.fromAnswer(versionAnswer);
        if (connectionStatus.isSuccess()){
            using(SourceConfigurationManager.class).update(request);
            using(SourceConfigurationManager.class).putProperty("version", versionAnswer.body);
        }

        return connectionStatus;
    }

}
