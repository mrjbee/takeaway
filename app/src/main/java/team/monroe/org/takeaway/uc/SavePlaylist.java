package team.monroe.org.takeaway.uc;

import org.monroe.team.corebox.services.ServiceRegistry;
import org.monroe.team.corebox.uc.UserCaseSupport;

import team.monroe.org.takeaway.manage.PlaylistStorage;
import team.monroe.org.takeaway.presentations.Playlist;

public class SavePlaylist extends UserCaseSupport<Playlist, Void>{

    public SavePlaylist(ServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }

    @Override
    protected Void executeImpl(Playlist request) {
       using(PlaylistStorage.class).putData(PlaylistStorage.PlaylistData.fromPlaylist(request));
       return null;
    }
}
