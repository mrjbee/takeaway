package team.monroe.org.takeaway;

import org.monroe.team.android.box.app.ApplicationSupport;

import team.monroe.org.takeaway.manage.SourceConfigurationManager;

public class App extends ApplicationSupport<AppModel> {
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

    public void updateConfiguration(SourceConfigurationManager.Configuration configuration) {
        model().usingService(SourceConfigurationManager.class).update(configuration);
    }
}
