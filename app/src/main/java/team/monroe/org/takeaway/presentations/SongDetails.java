package team.monroe.org.takeaway.presentations;

import java.io.Serializable;

public class SongDetails implements Serializable {

    public final String artist;
    public final String album;
    public final String title;

    public SongDetails(String artist, String album, String title) {
        this.artist = artist;
        this.album = album;
        this.title = title;
    }
}
