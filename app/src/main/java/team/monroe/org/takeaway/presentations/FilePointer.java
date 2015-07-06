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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FilePointer that = (FilePointer) o;

        if (!relativePath.equals(that.relativePath)) return false;
        if (!source.equals(that.source)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = source.hashCode();
        result = 31 * result + relativePath.hashCode();
        return result;
    }

    public static enum Type {
        FOLDER, FILE
    }

}
