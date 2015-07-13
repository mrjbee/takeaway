package team.monroe.org.takeaway.view;

import android.content.res.Resources;

import team.monroe.org.takeaway.presentations.FilePointer;

final public class FormatUtils {

    private FormatUtils() {}

    public static String getSongTitle(FilePointer filePointer, Resources resources) {
        String title = filePointer.details == null? null : filePointer.details.title;
        if (title ==  null){
            return filePointer.getNormalizedTitle();
        }else {
            return title;
        }
    }

    public static String getArtistAlbumString(FilePointer filePointer, Resources resources){
       String artist = filePointer.details == null? null : filePointer.details.artist;
       if (artist == null) return "";
       String album = filePointer.details.album;
       if (album != null){
           return "by " + artist + " from "+album;
       } else {
           return "by "+ artist;
       }
    }
}
