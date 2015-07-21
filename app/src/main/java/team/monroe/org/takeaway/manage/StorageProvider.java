package team.monroe.org.takeaway.manage;

import java.util.List;

import team.monroe.org.takeaway.manage.exceptions.FileOperationException;
import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.AwarePath;
import team.monroe.org.takeaway.presentations.Source;

public interface StorageProvider {

    public List<Source> sources() throws FileOperationException;
    public List<FilePointer> list(AwarePath awarePath) throws FileOperationException;
    public String absolutePath(FilePointer filePointer) throws FileOperationException;

}
