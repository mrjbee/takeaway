package team.monroe.org.takeaway.uc;

import org.monroe.team.corebox.app.Model;
import org.monroe.team.corebox.services.ServiceRegistry;
import org.monroe.team.corebox.uc.UserCaseSupport;

import java.util.ArrayList;
import java.util.List;

import team.monroe.org.takeaway.manage.DownloadManager;
import team.monroe.org.takeaway.manage.exceptions.ApplicationException;
import team.monroe.org.takeaway.manage.impl.LocalFileProvider;
import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.SongFile;

public class GetSoundFiles extends UserCaseSupport<List<FilePointer>, List<SongFile>>{

    public GetSoundFiles(ServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }

    @Override
    protected List<SongFile> executeImpl(List<FilePointer> request) {
        LocalFileProvider localFileProvider = using(LocalFileProvider.class);
        List<SongFile> answer = new ArrayList<>();
        SongFile itSongFile = null;
        DownloadManager downloadManager = using(DownloadManager.class);
        for (FilePointer filePointer : request) {
            itSongFile = localFileProvider.getSongFile(filePointer);
            if (itSongFile == null){
                //no local copy
                itSongFile = downloadManager.getExistsStreamFile(filePointer);
                if (itSongFile == null){
                    //no streaming file already
                    try {
                        DownloadManager.Transfer transfer = using(Model.class).execute(GetTransferForFile.class, filePointer);
                        itSongFile = downloadManager.createStreamFile(filePointer, transfer);
                    }catch (ApplicationException e){
                        itSongFile = new SongFile.NotAvailableSongFile(filePointer);
                    }
                }
            }
            answer.add(itSongFile);
        }
        return answer;
    }
}
