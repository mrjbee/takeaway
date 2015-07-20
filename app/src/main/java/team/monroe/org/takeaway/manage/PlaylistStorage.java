package team.monroe.org.takeaway.manage;

import org.json.JSONException;
import org.monroe.team.android.box.db.TextDataBase;
import org.monroe.team.android.box.json.Json;
import org.monroe.team.android.box.json.JsonBuilder;
import org.monroe.team.corebox.utils.P;

import java.util.ArrayList;
import java.util.List;

import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.Playlist;

public class PlaylistStorage extends TextDataBase.TextDataProvider<PlaylistStorage.PlaylistData>{

    public PlaylistStorage() {
        super(PlaylistTextDataTable.class, new PlaylistTextDataAdapter());
    }

    public static final class PlaylistTextDataTable extends TextDataBase.TextDataTable{
        @Override
        public String getDataName() {
            return "playlist";
        }
    }

    private static final class PlaylistTextDataAdapter implements TextDataBase.TextDataAdapter<PlaylistData> {

        @Override
        public PlaylistData toData(String id, String playlistJson) {
            if (playlistJson == null || id == null) return null;
            try {
               Json.JsonObject jsonPlaylistData = (Json.JsonObject) Json.createFromString(playlistJson);
               String title = jsonPlaylistData.asString("title");
               boolean autosave = jsonPlaylistData.value("autosave", Boolean.class);
               PlaylistData answer = new PlaylistData(title,id,autosave);
               Json.JsonArray array = jsonPlaylistData.asArray("song_list");
               for (int i=0;i<array.size();i++){
                   answer.songDataList.add(new SongData(
                      array.asObject(i).asString("path"),
                      array.asObject(i).asString("source_id")
                   ));
               }
               return answer;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public P<String, String> toIdText(PlaylistData data) {
            JsonBuilder.Array songArray = JsonBuilder.array();
            for (SongData songData : data.songDataList) {
                songArray.add(
                        JsonBuilder.object()
                                .field("path",songData.path)
                                .field("source_id", songData.source_id));
            }
            String json = JsonBuilder.build(JsonBuilder.object()
                        .field("title",data.title)
                        .field("autosave", data.autoSave)
                        .field("song_list", songArray)
            ).toJsonString();
            return new P<>(data.id, json);
        }

    }

    public static class PlaylistData{

        public final String title;
        public final String id;
        public final boolean autoSave;
        public final List<SongData> songDataList = new ArrayList<>();

        public PlaylistData(String title, String id, boolean autoSave) {
            this.title = title;
            this.id = id;
            this.autoSave = autoSave;
        }

        public static PlaylistData fromPlaylist(Playlist playlist){
            PlaylistData answer = new PlaylistData(playlist.title,playlist.title, true);
            for (FilePointer filePointer : playlist.songList) {
                answer.songDataList.add(new SongData(filePointer.relativePath, filePointer.source.id));
            }
            return answer;
        }
    }

    public static class SongData{

        public final String path;
        public final String source_id;

        public SongData(String path, String source_id) {
            this.path = path;
            this.source_id = source_id;
        }
    }
}
