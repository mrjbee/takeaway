package team.monroe.org.takeaway.db;

import org.monroe.team.android.box.db.Schema;

public class TakeAwaySchema extends Schema {

    public TakeAwaySchema() {
        super(1, "TakeAway.db", SongDetailsTable.class);
    }

    public static class SongDetailsTable extends VersionTable {

        public final String TABLE_NAME = "song";
        public final ColumnID<String> _SONG_ID= column("_song_id", String.class);
        public final ColumnID<String> _ARTIST= column("_artist", String.class);
        public final ColumnID<String> _ALBUM = column("_album", String.class);
        public final ColumnID<String> _TITLE = column("_title", String.class);

        public SongDetailsTable() {
            define(1,TABLE_NAME)
                    .column(_SONG_ID, "TEXT NOT NULL PRIMARY KEY")
                    .column(_ARTIST, "TEXT")
                    .column(_ALBUM, "TEXT")
                    .column(_TITLE, "TEXT");
        }

    }
}
