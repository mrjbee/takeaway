package team.monroe.org.takeaway.manage;

import org.monroe.team.android.box.db.TextDataBase;
import org.monroe.team.corebox.utils.P;

import java.util.ArrayList;

import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.Playlist;

public class PlaylistManager extends TextDataBase.TextDataProvider<Playlist>{

    public PlaylistManager() {
        super(PlaylistTextDataTable.class, new PlaylistTextDataAdapter());
    }

    public static final class PlaylistTextDataTable extends TextDataBase.TextDataTable{
        @Override
        public String getDataName() {
            return "playlist";
        }
    }

    private static final class PlaylistTextDataAdapter implements TextDataBase.TextDataAdapter<Playlist> {

        @Override
        public Playlist toData(String id, String playlistJson) {
            Playlist playlist = new Playlist(id, playlistJson, new ArrayList<FilePointer>());
            return playlist;
        }

        @Override
        public P<String, String> toIdText(Playlist playlist) {
            P<String,String> answer = new P<>(playlist.id, playlist.title);
            return answer;
        }
    }

}
