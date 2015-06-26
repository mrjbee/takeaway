package team.monroe.org.takeaway.manage;

import org.monroe.team.android.box.event.Event;
import org.monroe.team.android.box.event.GenericEvent;

final public class Events {
    private Events() {}
    public final static Event<CloudConnectionManager.ConnectionStatus> CLOUD_CONNECTION_STATUS = new GenericEvent<>("cloud.connection.status");
    public final static Event<Boolean> OFFLINE_MODE_CHANGED = new GenericEvent<>("offline_mode_changed");
}
