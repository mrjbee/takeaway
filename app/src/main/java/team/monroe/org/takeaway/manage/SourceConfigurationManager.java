package team.monroe.org.takeaway.manage;

import android.content.Context;

import org.monroe.team.android.box.utils.SerializationMap;

import java.io.Serializable;

public class SourceConfigurationManager {

    private final SerializationMap<String, Configuration> persist;

    public SourceConfigurationManager(Context context) {
        this.persist = new SerializationMap<>("source_conf", context);
    }

    public Configuration get(){
        return persist.get("conf");
    }

    public void update(Configuration configuration){
        persist.put("conf", configuration);
    }

    public static class Configuration implements Serializable{
        public final String host;
        public final int port;
        public final String user;
        public final String password;

        public Configuration(String host, int port, String user, String password) {
            this.host = host;
            this.port = port;
            this.user = user;
            this.password = password;
        }
    }
}
