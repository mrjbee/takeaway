package team.monroe.org.takeaway.uc;

import org.monroe.team.corebox.app.Model;
import org.monroe.team.corebox.services.ServiceRegistry;
import org.monroe.team.corebox.uc.UserCaseSupport;
import org.monroe.team.corebox.utils.Closure;
import org.monroe.team.corebox.utils.DateUtils;
import org.monroe.team.corebox.utils.Lists;

import java.util.ArrayList;
import java.util.List;

import team.monroe.org.takeaway.manage.PlaylistStorage;
import team.monroe.org.takeaway.manage.exceptions.ApplicationException;
import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.Playlist;
import team.monroe.org.takeaway.presentations.PlaylistAbout;
import team.monroe.org.takeaway.presentations.SongDetails;
import team.monroe.org.takeaway.presentations.Source;

public class PlaylistLoad extends UserCaseSupport<String, Playlist> {

    public PlaylistLoad(ServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }

    @Override
    protected Playlist executeImpl(String playlistId) {
        PlaylistStorage.PlaylistData data = using(PlaylistStorage.class).getData(playlistId);
        if (data == null) throw new IllegalStateException("No playlist with "+playlistId);
        Playlist playlist = new Playlist(data.id, data.title, new ArrayList<FilePointer>());
        playlist.autosave = data.autoSave;
        playlist.dateSaved = DateUtils.now().getTime();

        List<Source> sourceList = using(Model.class).execute(SourceGetAll.class, null);
        for (final PlaylistStorage.SongData songData : data.songDataList) {
            Source source = Lists.find(sourceList, new Closure<Source, Boolean>() {
                @Override
                public Boolean execute(Source arg) {
                    return arg.id.equals(songData.source_id);
                }
            });
            if (source == null){
                throw new ApplicationException(new NullPointerException("Couldn`t fins source with id = " + songData.source_id));
            }
            FilePointer filePointer = new FilePointer(source, songData.path, FilePointer.Type.FILE);
            filePointer.details = using(Model.class).execute(SongDetailsFromDB.class, filePointer.getSongId());
            playlist.songList.add(filePointer);
        }
        return playlist;
    }
}
