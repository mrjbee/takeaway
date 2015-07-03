package team.monroe.org.takeaway.presentations;

import java.io.File;

import team.monroe.org.takeaway.manage.DownloadManager;

public interface SongFile {

    FilePointer getFilePointer();
    void release();

    public static abstract class AbstractSongFile implements SongFile {

        private final FilePointer filePointer;

        protected AbstractSongFile(FilePointer filePointer) {
            this.filePointer = filePointer;
        }

        @Override
        final public FilePointer getFilePointer() {
            return filePointer;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AbstractSongFile)) return false;

            AbstractSongFile that = (AbstractSongFile) o;

            if (!filePointer.equals(that.filePointer)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return filePointer.hashCode();
        }
    }

    public static class NotAvailableSongFile extends AbstractSongFile {

        public NotAvailableSongFile(FilePointer filePointer) {
            super(filePointer);
        }

        @Override
        public void release() {}
    }

    public static class StreamFile extends AbstractSongFile implements Runnable{

        public DownloadManager.Priority priority;
        public final DownloadManager.Transfer transfer;
        public State state = State.NOT_STARTED;
        public final DownloadManager downloadManager;
        public File cacheFile;

        public StreamFile(FilePointer filePointer, DownloadManager.Priority priority, DownloadManager.Transfer transfer, DownloadManager downloadManager) {
            super(filePointer);
            this.priority = priority;
            this.transfer = transfer;
            this.downloadManager = downloadManager;
        }

        @Override
        public synchronized void release() {
            downloadManager.releaseStreamFile(this);
        }

        @Override
        public synchronized void run() {
            downloadManager.downloadInCache(this);
        }

        public boolean releaseSpace() {
            return !cacheFile.exists() || cacheFile.delete();
        }


        public enum State{
            NOT_STARTED,
            STARTED,
            @Deprecated
            RELEASED,
            FINISHED,
            ERROR
        }
    }
}
