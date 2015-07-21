package team.monroe.org.takeaway.presentations;

import java.io.File;
import java.io.Serializable;

public class FilePointer implements AwarePath, Serializable{

    public final Source source;
    public final String relativePath;
    private final String normalisedName;
    public final String name;
    public final Type type;
    public SongDetails details;

    public FilePointer(Source source, String relativePath, Type type) {
        this.source = source;
        this.relativePath = relativePath;
        this.name = fileSimpleName(relativePath);
        this.type = type;
        if (type == Type.FOLDER){
            normalisedName = name
                    .replace("_", " ")
                    .replace("-", " ")
                    .replaceAll(" +"," ")
                    .replaceAll("^ +","");
        } else {
            normalisedName = name
                    .replaceAll("\\.[^ .]+$", "")
                    .replaceFirst("^[0-9. ]+", "")
                    .replace("_", " ")
                    .replace("-", " ")
                    .replaceAll(" +"," ")
                    .replaceAll("^ +","");
        }
    }

    public String getNormalizedTitle(){
        return normalisedName;
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

    public String getSongId() {
       return source.id + ":"+relativePath;
    }

    @Override
    public Source getSource() {
        return source;
    }

    @Override
    public String getRelativePath() {
        return relativePath;
    }

    public static enum Type {
        FOLDER, FILE
    }

    private static String fileSimpleName(String path) {
        return new File(path).getName();
    }

}
