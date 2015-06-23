package team.monroe.org.takeaway.manage;

import android.content.Context;

import org.monroe.team.android.box.utils.SerializationMap;

import java.io.Serializable;

public class CloudConfigurationManager {

    private final SerializationMap<String, Serializable> persist;

    public CloudConfigurationManager(Context context) {
        this.persist = new SerializationMap<>("source_conf", context);
    }

    public Configuration get(){
        return (Configuration) persist.get("configuration");
    }

    public void update(Configuration configuration){
        persist.put("configuration", configuration);
    }

    public void putProperty(String key, Serializable value){
        persist.put(key, value);
    }

    public <Type> Type getProperty(String key){
        return (Type) persist.get(key);
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
