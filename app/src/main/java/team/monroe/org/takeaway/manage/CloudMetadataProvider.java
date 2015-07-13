package team.monroe.org.takeaway.manage;


import team.monroe.org.takeaway.manage.exceptions.FileOperationException;
import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.SongDetails;

public interface CloudMetadataProvider {
    SongDetails getFiledDetails(FilePointer filePointer) throws FileOperationException;
}
