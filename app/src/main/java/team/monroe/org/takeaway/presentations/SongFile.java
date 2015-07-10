package team.monroe.org.takeaway.presentations;

import java.io.File;

import team.monroe.org.takeaway.manage.DownloadManager;

public interface SongFile {

    FilePointer getFilePointer();
    void release();
    String getDataSourcePath();
    boolean isReady();
    boolean isBroken();

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

        @Override
        public String getDataSourcePath() {
            throw new IllegalStateException("Not Available");
        }

        @Override
        public boolean isReady() {
            throw new IllegalStateException("Not Available");
        }

        @Override
        public boolean isBroken() {
            return true;
        }
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
        public String getDataSourcePath() {
            if (cacheFile != null) return cacheFile.getAbsolutePath();
            return null;
        }

        @Override
        public boolean isReady() {
            return state == State.FINISHED || state == State.ERROR;
        }

        @Override
        public boolean isBroken() {
            return state == State.ERROR;
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
            FINISHED,
            ERROR,
            INTERRUPTED
        }
    }
}
