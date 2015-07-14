package team.monroe.org.takeaway.view;

import android.content.res.Resources;

import org.monroe.team.corebox.utils.DateUtils;

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

    public static String getArtistString(FilePointer filePointer, Resources resources) {
        if (filePointer.details == null || filePointer.details.artist == null || filePointer.details.artist.isEmpty()){
            return "Unknown Artist";
        }else {
            return filePointer.details.artist;
        }
    }

    public static String getAlbumString(FilePointer filePointer, Resources resources) {
        if (filePointer.details == null || filePointer.details.album == null || filePointer.details.album.isEmpty()){
            return "Unknown Album";
        }else {
            return filePointer.details.album;
        }
    }

    public static String getArtistString(FilePointer filePointer, String fallbackString) {
        if (filePointer.details == null || filePointer.details.album == null || filePointer.details.album.isEmpty()){
            return fallbackString;
        }else {
            return filePointer.details.artist;
        }
    }

    public static String toTimeString(long timeMs) {
        long[] periods = DateUtils.splitperiod(timeMs);
        return asDoubleDigitString(periods[2])+":"+asDoubleDigitString(periods[3]);
    }

    private static String asDoubleDigitString(Number period) {
        String string = period.toString();
        if (string.length() == 1){
            return "0"+string;
        }else {
           return string;
        }
    }
}
