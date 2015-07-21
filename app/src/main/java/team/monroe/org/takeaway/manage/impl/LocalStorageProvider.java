package team.monroe.org.takeaway.manage.impl;

import java.util.Collections;
import java.util.List;

import team.monroe.org.takeaway.manage.StorageProvider;
import team.monroe.org.takeaway.manage.exceptions.FileOperationException;
import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.AwarePath;
import team.monroe.org.takeaway.presentations.SongFile;
import team.monroe.org.takeaway.presentations.Source;


public class LocalStorageProvider implements StorageProvider {

    @Override
    public List<Source> sources() throws FileOperationException {
        return Collections.emptyList();
    }

    @Override
    public List<FilePointer> list(AwarePath awarePath) throws FileOperationException {
        return Collections.emptyList();
    }


    @Override
    public String absolutePath(FilePointer filePointer) throws FileOperationException {
        return null;
    }


    public SongFile getSongFile(FilePointer pointer){
        return null;
    }
}
