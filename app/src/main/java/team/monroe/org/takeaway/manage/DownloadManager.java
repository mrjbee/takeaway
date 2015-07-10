package team.monroe.org.takeaway.manage;

import android.content.Context;
import android.os.Environment;

import org.monroe.team.android.box.event.Event;
import org.monroe.team.corebox.log.L;
import org.monroe.team.corebox.utils.Closure;
import org.monroe.team.corebox.utils.Lists;
import org.monroe.team.corebox.utils.P;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.SongFile;

public class DownloadManager {

    private L.Logger log = L.create("DOWNLOAD_MANAGER");

    private final Context context;
    private final File mCacheFolder;

    private final List<SongFile.StreamFile> mStreamFilesRequestList = new ArrayList<>();

    private ExecutorService mCacheDownloadExecutor = Executors.newFixedThreadPool(2);
    private P<Future, SongFile.StreamFile> mCurrentCacheDownload;

    public DownloadManager(Context context) {
        this.context = context;
        File cacheRootFile = chooseCacheFolder(context);
        mCacheFolder = new File(cacheRootFile, "online");
        if (!mCacheFolder.exists() && !mCacheFolder.mkdirs()){
           throw new IllegalStateException("Could not create cache folder");
        }
    }

    private File chooseCacheFolder(Context context) {
        File sdcard = Environment.getExternalStorageDirectory();
        if (sdcard.exists()){
            File answer = new File(sdcard, "TakeAway");
            if (answer.exists() || answer.mkdirs()){
                return answer;
            }
        }
        return context.getExternalCacheDir();
    }

    public synchronized SongFile createStreamFile(FilePointer filePointer, Transfer transfer, Priority priority) {
        SongFile.StreamFile streamFile = (SongFile.StreamFile) renewExistingStreamFile(filePointer, priority);
        if (streamFile == null){
            streamFile = new SongFile.StreamFile(filePointer, priority, transfer, this);
            mStreamFilesRequestList.add(streamFile);
        }
        return streamFile;
    }


    public synchronized SongFile renewExistingStreamFile(FilePointer filePointer, Priority priority) {
        for (SongFile.StreamFile streamFile : mStreamFilesRequestList) {
                if (streamFile.getFilePointer().equals(filePointer)){
                    streamFile.priority = priority;
                    return streamFile;
                }
        }
        return null;
    }

    public synchronized void reorderStreamFileQueue() {

        Collections.sort(mStreamFilesRequestList, new Comparator<SongFile.StreamFile>() {
            @Override
            public int compare(SongFile.StreamFile lhs, SongFile.StreamFile rhs) {
                return lhs.priority.compareTo(rhs.priority);
            }
        });

        SongFile.StreamFile candidateStreamFile = Lists.find(mStreamFilesRequestList, new Closure<SongFile.StreamFile, Boolean>() {
            @Override
            public Boolean execute(SongFile.StreamFile arg) {
                return arg.state != SongFile.StreamFile.State.FINISHED;
            }
        });

        if (mCurrentCacheDownload != null) {
            if (candidateStreamFile == null || mCurrentCacheDownload.second == candidateStreamFile) {
                return;
            }
            mCurrentCacheDownload.first.cancel(true);
        }

        scheduleCacheDownloading(candidateStreamFile);
    }

    private void scheduleCacheDownloading(SongFile.StreamFile candidateStreamFile) {
        Future future = mCacheDownloadExecutor.submit(candidateStreamFile);
        mCurrentCacheDownload = new P<>(future, candidateStreamFile);
    }


    public synchronized void updateStreamFilePriority(Priority priority) {
        for (SongFile.StreamFile streamFile : mStreamFilesRequestList) {
            streamFile.priority = priority;
        }
    }

    public synchronized void releaseStreamFile(SongFile.StreamFile streamFile) {
        log.d("Release cached file "+streamFile.getFilePointer().relativePath +" = "+streamFile.state);
        int index = mStreamFilesRequestList.indexOf(streamFile);
        if (index == -1){
            log.e("Release cached file [ERROR] "+streamFile.getFilePointer().relativePath);
            throw new IllegalStateException("Unknown stream file");
        }

        mStreamFilesRequestList.remove(index);
        if (streamFile.state == SongFile.StreamFile.State.FINISHED){
            log.d("Release cached file [REMOVE]: "+streamFile.getFilePointer().relativePath);
            File cacheFile = toCacheFile(streamFile);
            cacheFile.delete();
        }else {
            log.d("Release cached file [SKIPPED] "+streamFile.getFilePointer().relativePath);
        }
    }

    public void downloadInCache(SongFile.StreamFile streamFile) {
        log.d("Start actual downloading "+streamFile.getFilePointer().relativePath);
        if (streamFile.state == SongFile.StreamFile.State.FINISHED){
            log.d("File already downloaded "+streamFile.getFilePointer().relativePath);
            reorderStreamFileQueue();
            return;
        }
        streamFile.state = SongFile.StreamFile.State.STARTED;
        File cacheFile = toCacheFile(streamFile);
        File cacheFileFolder = cacheFile.getParentFile();
        if (!cacheFileFolder.exists() && !cacheFileFolder.mkdirs()){
            log.w("Coulnot create folder", cacheFileFolder.getAbsolutePath());
            streamFile.state = SongFile.StreamFile.State.ERROR;
            notifyFilePreparing(streamFile, false);
            reorderStreamFileQueue();
            return;
        }

        streamFile.cacheFile = cacheFile;
        streamFile.releaseSpace();

        FileOutputStream output = null;
        try {
            log.d("Open stream "+streamFile.getFilePointer().relativePath);
            InputStream inputStream = streamFile.transfer.getInputStream();
            output = new FileOutputStream(streamFile.cacheFile);
            byte data[] = new byte[4096];
            int count;
            while ((count = inputStream.read(data)) != -1) {
                if (Thread.currentThread().isInterrupted()) {
                    log.d("Downloading interrupted "+streamFile.getFilePointer().relativePath);
                    streamFile.state = SongFile.StreamFile.State.INTERRUPTED;
                    streamFile.transfer.releaseInput();
                    output.close();
                    streamFile.releaseSpace();
                    return;
                }
                output.write(data, 0, count);
            }

        } catch (Exception e) {
            log.e("Error during downloading "+streamFile.getFilePointer().relativePath, e);
            streamFile.state = SongFile.StreamFile.State.ERROR;
            streamFile.releaseSpace();
            streamFile.transfer.releaseInput();
            notifyFilePreparing(streamFile, false);
            reorderStreamFileQueue();
        }

        if (output != null) try {
            output.close();
        } catch (IOException whocare) {}

        log.d("Downloading finished "+streamFile.getFilePointer().relativePath);
        streamFile.transfer.releaseInput();
        streamFile.state = SongFile.StreamFile.State.FINISHED;
        notifyFilePreparing(streamFile, true);
        reorderStreamFileQueue();
    }

    private void notifyFilePreparing(SongFile.StreamFile streamFile, boolean successful) {
        Event.send(context, Events.FILE_PREPARED, new P<FilePointer, Boolean>(streamFile.getFilePointer(), successful));
    }

    private File toCacheFile(SongFile.StreamFile streamFile) {
        File answer = new File(mCacheFolder, streamFile.getFilePointer().source.id);
        answer = new File(answer, streamFile.getFilePointer().relativePath);
        return answer;
    }


    public interface Transfer {
        InputStream getInputStream() throws TransferFailException;
        void releaseInput();
    }

    public static class TransferFailException extends Exception{
        public TransferFailException() {}
        public TransferFailException(String detailMessage) {
            super(detailMessage);
        }
        public TransferFailException(String detailMessage, Throwable throwable) {super(detailMessage, throwable);}
        public TransferFailException(Throwable throwable) {
            super(throwable);
        }
    }

    public static enum Priority{
        HIGHEST, HIGH, MEDIUM, LOW
    }
}
