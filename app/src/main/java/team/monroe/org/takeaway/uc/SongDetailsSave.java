package team.monroe.org.takeaway.uc;

import org.monroe.team.android.box.db.TransactionUserCase;
import org.monroe.team.corebox.services.ServiceRegistry;
import org.monroe.team.corebox.utils.P;

import team.monroe.org.takeaway.db.Dao;
import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.SongDetails;

public class SongDetailsSave extends TransactionUserCase<FilePointer, Void, Dao> {

    public SongDetailsSave(ServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }

    @Override
    protected Void transactionalExecute(FilePointer request, Dao dao) {
        dao.insertSongDetails(request.getSongId(), request.details.artist, request.details.album, request.details.title);
        return null;
    }
}
