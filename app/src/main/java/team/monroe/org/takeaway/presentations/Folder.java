package team.monroe.org.takeaway.presentations;

import java.io.Serializable;

public class Folder implements Serializable{

    public final static Folder FOLDER_ROOT = new Folder("root","");

    public final String id;
    public final String title;

    public Folder(String id, String title) {
        this.id = id;
        this.title = title;
    }
}
