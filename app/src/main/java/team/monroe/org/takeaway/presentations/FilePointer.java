package team.monroe.org.takeaway.presentations;

import java.io.Serializable;

public class FilePointer implements Serializable {

    public final Source source;
    public final String relativePath;
    public final String name;
    public final Type type;

    public FilePointer(Source source, String relativePath, String name, Type type) {
        this.source = source;
        this.relativePath = relativePath;
        this.name = name;
        this.type = type;
    }

    public String getNormalizedTitle(){
       return name.replace(".mp3","").replace("_", " ").replace("-", " ").replaceAll(" +"," ");
    }

    public static enum Type {
        FOLDER, FILE
    }

}
