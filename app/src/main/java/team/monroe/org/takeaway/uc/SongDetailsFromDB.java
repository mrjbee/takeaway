package team.monroe.org.takeaway.uc;

import org.monroe.team.android.box.db.DAOSupport;
import org.monroe.team.android.box.db.TransactionUserCase;
import org.monroe.team.corebox.services.ServiceRegistry;

import team.monroe.org.takeaway.db.Dao;
import team.monroe.org.takeaway.presentations.SongDetails;

public class SongDetailsFromDB extends TransactionUserCase<String, SongDetails, Dao> {

    public SongDetailsFromDB(ServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }

    @Override
    protected SongDetails transactionalExecute(String request, Dao dao) {
        DAOSupport.Result result = dao.getSongDetail(request);
        return result == null ? null: createFromDBResult(result);
    }

    private SongDetails createFromDBResult(DAOSupport.Result result) {
        return new SongDetails(
                result.get(1, String.class),
                result.get(2, String.class),
                result.get(3, String.class));
    }
}
