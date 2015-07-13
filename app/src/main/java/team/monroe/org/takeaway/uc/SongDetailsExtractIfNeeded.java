package team.monroe.org.takeaway.uc;

import android.media.MediaMetadataRetriever;

import org.monroe.team.corebox.app.Model;
import org.monroe.team.corebox.services.ServiceRegistry;
import org.monroe.team.corebox.uc.UserCaseSupport;
import org.monroe.team.corebox.utils.P;
import team.monroe.org.takeaway.presentations.SongDetails;
import team.monroe.org.takeaway.presentations.SongFile;

public class SongDetailsExtractIfNeeded extends UserCaseSupport<SongFile, SongDetails> {

    public SongDetailsExtractIfNeeded(ServiceRegistry serviceRegistry) {
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
    protected SongDetails executeImpl(SongFile request) {
        SongDetails answer = using(Model.class).execute(SongDetailsFromDB.class, request.getFilePointer().getSongId());
        if (answer == null){
            answer = getSongDetails(request);
            request.getFilePointer().details = answer;
            using(Model.class).execute(SongDetailsSave.class, request.getFilePointer());
        }
        return answer;
    }

}
