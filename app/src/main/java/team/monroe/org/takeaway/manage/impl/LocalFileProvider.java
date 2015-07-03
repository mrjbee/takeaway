package team.monroe.org.takeaway.manage.impl;

import java.util.List;

import team.monroe.org.takeaway.manage.FileProvider;
import team.monroe.org.takeaway.manage.exceptions.FileOperationException;
import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.SongFile;
import team.monroe.org.takeaway.presentations.Source;

public class LocalFileProvider implements FileProvider {

    @Override
    public List<Source> getSources() throws FileOperationException {
        return null;
    }

    @Override
    public List<FilePointer> getNestedFiles(FilePointer filePointer) throws FileOperationException {
        return null;
    }

    @Override
    public String getFileId(FilePointer filePointer) throws FileOperationException {
        return null;
    }


    public SongFile getSongFile(FilePointer pointer){
        return null;
    }
}
