package team.monroe.org.takeaway.manage;

import android.util.Pair;

import org.monroe.team.android.box.event.Event;
import org.monroe.team.android.box.event.GenericEvent;
import org.monroe.team.corebox.utils.P;

import team.monroe.org.takeaway.presentations.FilePointer;

final public class Events {
    private Events() {}
    public final static Event<CloudConnectionManager.ConnectionStatus> CLOUD_CONNECTION_STATUS = new GenericEvent<>("cloud.connection.status");
    public final static Event<Boolean> OFFLINE_MODE_CHANGED = new GenericEvent<>("offline_mode_changed");
    public final static Event<P<FilePointer, Boolean>> FILE_PREPARED = new GenericEvent<>("file_prepared");

}
