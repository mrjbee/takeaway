package team.monroe.org.takeaway.uc;

import android.media.MediaMetadataRetriever;

import org.monroe.team.android.box.db.DAOSupport;
import org.monroe.team.android.box.db.TransactionUserCase;
import org.monroe.team.corebox.app.Model;
import org.monroe.team.corebox.services.ServiceRegistry;

import java.io.File;

import team.monroe.org.takeaway.db.Dao;
import team.monroe.org.takeaway.presentations.SongDetails;
import team.monroe.org.takeaway.presentations.SongFile;

public class ExploreSongDetails extends TransactionUserCase<SongFile, SongDetails, Dao> {

    public ExploreSongDetails(ServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }

    private SongDetails getSongDetails(SongFile request) {
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        try {
            metadataRetriever.setDataSource(request.getDataSourcePath());
            String artist = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String album = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            String title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            return new SongDetails(artist, album, title);
        } finally {
            metadataRetriever.release();
        }
    }

    @Override
    protected SongDetails transactionalExecute(SongFile request, Dao dao) {
        SongDetails answer = using(Model.class).execute(GetSongDetailsFromDB.class, request.getFilePointer().getSongId());
        if (answer == null){
            answer = getSongDetails(request);
            dao.insertSongDetails(request.getFilePointer().getSongId(), answer.artist, answer.album, answer.title);
        }
        return answer;
    }

}
