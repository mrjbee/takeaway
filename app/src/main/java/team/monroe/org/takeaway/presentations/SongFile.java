package team.monroe.org.takeaway.presentations;

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

    public static class StreamFile extends AbstractSongFile{

        private final DownloadManager.Transfer transfer;

        public StreamFile(FilePointer filePointer, DownloadManager.Transfer transfer) {
            super(filePointer);
            this.transfer = transfer;
        }

        @Override
        public void release() {

        }
    }
}
