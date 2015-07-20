package team.monroe.org.takeaway.presentations;

import org.monroe.team.corebox.utils.DateUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Playlist implements Serializable{

    public final String id;
    public String title;
    public boolean autosave = false;
    public final List<FilePointer> songList;
    public long dateModified = 0;
    public long dateSaved = 0;


    public Playlist(String id, String title, List<FilePointer> songList) {
        this.id = id;
        this.title = title;
        this.songList = songList;
    }

    @Override
    public String toString() {
        return "Playlist{" +
                "id='" + id + '\'' +
                ", songList=" + songList +
                '}';
    }

    public boolean isSaveRequired() {
        return title != null && dateModified > dateSaved;
    }
}
