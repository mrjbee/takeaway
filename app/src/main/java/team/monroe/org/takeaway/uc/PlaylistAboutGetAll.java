package team.monroe.org.takeaway.uc;

import org.monroe.team.corebox.services.ServiceRegistry;
import org.monroe.team.corebox.uc.UserCaseSupport;
import org.monroe.team.corebox.utils.Closure;
import org.monroe.team.corebox.utils.Lists;

import java.util.List;

import team.monroe.org.takeaway.manage.PlaylistStorage;
import team.monroe.org.takeaway.presentations.PlaylistAbout;

public class PlaylistAboutGetAll extends UserCaseSupport<Void, List<PlaylistAbout>> {

    public PlaylistAboutGetAll(ServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }

    @Override
    protected List<PlaylistAbout> executeImpl(Void request) {
        List<PlaylistStorage.PlaylistData> descriptions = using(PlaylistStorage.class).getValueList();
        return Lists.collect(descriptions, new Closure<PlaylistStorage.PlaylistData, PlaylistAbout>() {
            @Override
            public PlaylistAbout execute(PlaylistStorage.PlaylistData arg) {
                return new PlaylistAbout(arg.id,arg.title,arg.songDataList.size());
            }
        });
    }

}
