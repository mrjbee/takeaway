package team.monroe.org.takeaway.presentations;

import org.monroe.team.corebox.utils.DateUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Playlist implements Serializable{

    public final String id;
    public final String title;
    public final List<FilePointer> songList;


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
}
