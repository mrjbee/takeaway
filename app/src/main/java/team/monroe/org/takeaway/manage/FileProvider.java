package team.monroe.org.takeaway.manage;

import java.io.FileNotFoundException;
import java.util.List;

import team.monroe.org.takeaway.manage.exceptions.FileOperationException;
import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.Source;

public interface FileProvider {

    public List<Source> getSources() throws FileOperationException;
    public List<FilePointer> getNestedFiles(FilePointer filePointer) throws FileOperationException;

}
