package team.monroe.org.takeaway.uc;

import org.monroe.team.corebox.app.Model;
import org.monroe.team.corebox.services.ServiceRegistry;
import org.monroe.team.corebox.uc.UserCaseSupport;

import java.util.ArrayList;
import java.util.List;

import team.monroe.org.takeaway.manage.DownloadManager;
import team.monroe.org.takeaway.manage.exceptions.ApplicationException;
import team.monroe.org.takeaway.manage.impl.LocalStorageProvider;
import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.SongFile;

public class SoundFileGet extends UserCaseSupport<List<FilePointer>, List<SongFile>>{

    public SoundFileGet(ServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }

    @Override
    protected List<SongFile> executeImpl(List<FilePointer> request) {
        LocalStorageProvider localFileProvider = using(LocalStorageProvider.class);
        List<SongFile> answer = new ArrayList<>();
        SongFile itSongFile = null;
        DownloadManager downloadManager = using(DownloadManager.class);
        downloadManager.streamFileQueuePriorityUpdate(DownloadManager.Priority.LOW);
        for (int i=0;i<request.size();i++) {
            FilePointer filePointer =  request.get(i);
            itSongFile = localFileProvider.getSongFile(filePointer);
            if (itSongFile == null){
                //no local copy
                itSongFile = downloadManager.streamFileFromCache(filePointer, getPriorityByIndex(i));
                if (itSongFile == null){
                    //no streaming file already
                    try {
                        DownloadManager.Transfer transfer = using(Model.class).execute(TransferGetForFile.class, filePointer);
                        itSongFile = downloadManager.streamFileCreate(filePointer, transfer, getPriorityByIndex(i));
                    }catch (ApplicationException e){
                        itSongFile = new SongFile.NotAvailableSongFile(filePointer);
                    }
                }
            }
            answer.add(itSongFile);
        }
        downloadManager.streamFileStartNextDownloading();
        return answer;
    }

    private DownloadManager.Priority getPriorityByIndex(int i) {
        switch (i){
            case 0: return DownloadManager.Priority.HIGHEST;
            case 1:
            case 2: return DownloadManager.Priority.HIGH;
            default: return DownloadManager.Priority.MEDIUM;
        }
    }
}
