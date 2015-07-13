package team.monroe.org.takeaway.presentations;

import java.io.Serializable;
import java.util.regex.Pattern;

public class FilePointer implements Serializable {

    public final Source source;
    public final String relativePath;
    public final String normolizedName;
    public final String name;
    public final Type type;

    public FilePointer(Source source, String relativePath, String name, Type type) {
        this.source = source;
        this.relativePath = relativePath;
        this.name = name;
        this.type = type;
        if (type == Type.FOLDER){
            normolizedName = name
                    .replace("_", " ")
                    .replace("-", " ")
                    .replaceAll(" +"," ")
                    .replaceAll("^ +","");
        }else {
            normolizedName = name
                    .replaceAll("\\.[^ .]+$", "")
                    .replaceFirst("^[0-9]+", "")
                    .replace("_", " ")
                    .replace("-", " ")
                    .replaceAll(" +"," ")
                    .replaceAll("^ +","");
        }
    }

    public String getNormalizedTitle(){
        return normolizedName;
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
