package team.monroe.org.takeaway.presentations;

import java.io.Serializable;

public class Folder implements Serializable{

    public final static Folder FOLDER_ROOT = new Folder("");

    public final String path;

    public Folder(String path) {
        this.path = path;
    }

}
