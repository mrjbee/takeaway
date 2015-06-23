package team.monroe.org.takeaway.presentations;

import java.io.Serializable;

public class Source implements Serializable {

    public final String id;
    public final String title;

    public Source(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public FilePointer asFilePointer() {
        return new FilePointer(this,"",title, FilePointer.Type.FOLDER);
    }
}
