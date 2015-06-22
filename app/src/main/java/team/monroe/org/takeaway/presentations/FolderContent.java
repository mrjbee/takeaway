package team.monroe.org.takeaway.presentations;

import java.io.Serializable;
import java.util.List;

public class FolderContent implements Serializable{

    public final Folder folder;
    public final List<Folder> subFolders;

    public FolderContent(Folder folder, List<Folder> subFolders) {
        this.folder = folder;
        this.subFolders = subFolders;
    }
}
