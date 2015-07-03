package team.monroe.org.takeaway.manage;

import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.SongFile;

public class DownloadManager {

    public SongFile createStreamFile(FilePointer filePointer, Transfer transfer) {

        SongFile answer = new SongFile.StreamFile(filePointer, transfer);
        return answer;
    }

    public SongFile getExistsStreamFile(FilePointer filePointer) {
        return null;
    }

    public interface Transfer {}
}
