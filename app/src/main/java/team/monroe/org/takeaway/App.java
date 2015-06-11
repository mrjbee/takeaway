package team.monroe.org.takeaway;

import org.monroe.team.android.box.app.ApplicationSupport;

public class App extends ApplicationSupport<AppModel> {
    @Override
    protected AppModel createModel() {
        return new AppModel("takeaway", getApplicationContext());
    }
}
