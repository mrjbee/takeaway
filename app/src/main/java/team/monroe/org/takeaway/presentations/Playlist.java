package team.monroe.org.takeaway.presentations;

import org.monroe.team.corebox.utils.DateUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Playlist implements Serializable{

    public final String title;
    public final String id;
    public final List<FilePointer> songList;


    public Playlist(String title, String id, List<FilePointer> songList) {
        this.title = title;
        this.id = id;
        this.songList = songList;
    }

    public Playlist duplicate() {
        return new Playlist(title, id + DateUtils.msAsString(), new ArrayList<>(songList));
    }

    @Override
    public String toString() {
        return "Playlist{" +
                "title='" + title + '\'' +
                ", id='" + id + '\'' +
                ", songList=" + songList +
                '}';
    }
}
