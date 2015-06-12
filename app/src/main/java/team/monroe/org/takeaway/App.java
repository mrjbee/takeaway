package team.monroe.org.takeaway;

import org.monroe.team.android.box.app.ApplicationSupport;
import org.monroe.team.android.box.utils.AndroidLogImplementation;
import org.monroe.team.corebox.log.L;

import team.monroe.org.takeaway.manage.SourceConfigurationManager;
import team.monroe.org.takeaway.presentations.SourceConnectionStatus;
import team.monroe.org.takeaway.uc.CheckSourceConnection;

public class App extends ApplicationSupport<AppModel> {

    static {
        L.setup(new AndroidLogImplementation());
    }

    @Override
    protected AppModel createModel() {
        return new AppModel("takeaway", getApplicationContext());
    }

    public boolean isSourceConfigured() {
        return model().usingService(SourceConfigurationManager.class).get() != null;
    }

    public SourceConfigurationManager.Configuration getSourceConfiguration() {
        return model().usingService(SourceConfigurationManager.class).get();
    }

    public void updateConfiguration(SourceConfigurationManager.Configuration configuration, ValueObserver<SourceConnectionStatus> observer) {
        fetchValue(CheckSourceConnection.class, configuration, new NoOpValueAdapter<SourceConnectionStatus>(), observer);
    }
}
