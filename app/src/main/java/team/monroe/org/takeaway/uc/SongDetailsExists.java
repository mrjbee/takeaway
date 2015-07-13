package team.monroe.org.takeaway.uc;

import org.monroe.team.android.box.db.TransactionUserCase;
import org.monroe.team.corebox.services.ServiceRegistry;
import org.monroe.team.corebox.utils.P;

import team.monroe.org.takeaway.db.Dao;
import team.monroe.org.takeaway.presentations.SongDetails;

public class SongDetailsExists extends TransactionUserCase<String, Boolean, Dao> {

    public SongDetailsExists(ServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }

    @Override
    protected Boolean transactionalExecute(String request, Dao dao) {
        return dao.getSongDetail(request) != null;
    }
}
