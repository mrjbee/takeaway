package team.monroe.org.takeaway.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.monroe.team.android.box.db.DAOSupport;
import org.monroe.team.android.box.db.Schema;
import org.monroe.team.corebox.utils.Closure;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Dao extends DAOSupport {

    public Dao(SQLiteDatabase db, Schema schema) {
        super(db, schema);
    }

    public long insertSongDetails(String songId, String artist, String album, String title) {

        long id = db.insertOrThrow(
                songTable().TABLE_NAME,
                null,
                content()
                        .value(songTable()._SONG_ID, songId)
                        .value(songTable()._ALBUM, album)
                        .value(songTable()._ARTIST, artist)
                        .value(songTable()._TITLE, title)
                        .get());
        return id;
    }

    public Result getSongDetail(String songId) {
        String whereStatement = null;
        String[] whereArgs = null;
        whereStatement = songTable()._SONG_ID.name()+" is ?";
        whereArgs = strs(songId);

        Cursor cursor = db.query(songTable().TABLE_NAME,
                strs(songTable()._SONG_ID.name(),
                        songTable()._ARTIST.name(),
                        songTable()._ALBUM.name(),
                        songTable()._TITLE.name()),
                whereStatement,
                whereArgs,
                null,
                null,
                null);
        List<Result> list = collect(cursor, new Closure<Cursor, Result>() {
            @Override
            public Result execute(Cursor arg) {
                return result().with(
                        arg.getString(0),
                        arg.isNull(1) ? null:arg.getString(1),
                        arg.isNull(2) ? null:arg.getString(2),
                        arg.isNull(3) ? null:arg.getString(3));
            }
        });

        if (list.size() > 1) throw new IllegalStateException("Two much details");

        if (list.isEmpty()) return null;
        else return list.get(0);
    }

    private List<Result> collect(Cursor cursor, Closure<Cursor,Result> closure) {
        List<Result> answer = new ArrayList<Result>(cursor.getCount());
        Result itResult;
        while (cursor.moveToNext()) {
            itResult = closure.execute(cursor);
            if (itResult != null) answer.add(itResult);
        }
        cursor.close();
        return answer;
    }


    private TakeAwaySchema.SongDetailsTable songTable() {
        return table(TakeAwaySchema.SongDetailsTable.class);
    }

}
