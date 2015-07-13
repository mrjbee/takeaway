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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
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

    private final List<P<String, WeakReference<SongFile.StreamFile>>> mStreamFileCache = new ArrayList<>();

    private ExecutorService mCacheDownloadExecutor = Executors.newFixedThreadPool(2);
    private P<Future, SongFile.StreamFile> mCurrentCacheDownload;


    public DownloadManager(Context context) {
        this.context = context;
        File cacheRootFile = _chooseCacheFolder(context);
        mCacheFolder = new File(cacheRootFile, "online");
        delete(mCacheFolder);
        if (!mCacheFolder.exists() && !mCacheFolder.mkdirs()){
           throw new IllegalStateException("Could not create cache folder");
        }
    }

    private void delete(File folder) {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()){
                delete(file);
            }else {
                file.delete();
            }
        }
        folder.delete();
    }

    private File _chooseCacheFolder(Context context) {
        File sdcard = Environment.getExternalStorageDirectory();
        if (sdcard.exists()){
            File answer = new File(sdcard, "TakeAway");
            if (answer.exists() || answer.mkdirs()){
                return answer;
            }
        }
        return context.getExternalCacheDir();
    }

    public synchronized SongFile streamFileCreate(FilePointer filePointer, Transfer transfer, Priority priority) {
        SongFile.StreamFile streamFile = (SongFile.StreamFile) streamFileFromCache(filePointer, priority);
        if (streamFile == null){
            File cacheFile = asCacheFile(filePointer);
            streamFile = new SongFile.StreamFile(filePointer, priority, transfer, this, cacheFile);
            mStreamFileCache.add(new P<String, WeakReference<SongFile.StreamFile>>(cacheFile.getAbsolutePath(), new WeakReference<SongFile.StreamFile>(streamFile)));
        }
        return streamFile;
    }


    public synchronized SongFile streamFileFromCache(FilePointer filePointer, Priority priority) {
        for (P<String, WeakReference<SongFile.StreamFile>> stringWeakReferenceP : mStreamFileCache) {
            SongFile.StreamFile streamFile = stringWeakReferenceP.second.get();
                if (streamFile != null && streamFile.getFilePointer().equals(filePointer)){
                    streamFile.priority = priority;
                    return streamFile;
                }
        }
        return null;
    }

    public synchronized void streamFileStartNextDownloading() {
        Collections.sort(mStreamFileCache, new Comparator<P<String, WeakReference<SongFile.StreamFile>>>() {
            @Override
            public int compare(P<String, WeakReference<SongFile.StreamFile>> lhs, P<String, WeakReference<SongFile.StreamFile>> rhs) {
                SongFile.StreamFile lsf = lhs.second.get();
                SongFile.StreamFile rsf = rhs.second.get();
                if (lsf == null) return -1;
                if (rsf == null) return 1;
                return lsf.priority.compareTo(rsf.priority);
            }
        });

        P<String, WeakReference<SongFile.StreamFile>> candidateStreamFileP = Lists.find(mStreamFileCache, new Closure<P<String, WeakReference<SongFile.StreamFile>>, Boolean>() {
            @Override
            public Boolean execute(P<String, WeakReference<SongFile.StreamFile>> arg) {
                SongFile.StreamFile file = arg.second.get();
                return  file != null && file.state != SongFile.StreamFile.State.FINISHED;
            }
        });

        SongFile.StreamFile candidateStreamFile = candidateStreamFileP == null? null: candidateStreamFileP.second.get();

        //Check if current downloading not equals to current downloading
        if (mCurrentCacheDownload != null) {
            if (candidateStreamFile == null || mCurrentCacheDownload.second == candidateStreamFile) {
                return;
            }
            mCurrentCacheDownload.first.cancel(true);
        }
        if (candidateStreamFile == null) return;
        _scheduleCacheDownloading(candidateStreamFile);
    }

    private synchronized void clearCache() {
        Lists.iterateAndRemove(mStreamFileCache, new Closure<Iterator<P<String, WeakReference<SongFile.StreamFile>>>, Boolean>() {
            @Override
            public Boolean execute(Iterator<P<String, WeakReference<SongFile.StreamFile>>> arg) {
                P<String, WeakReference<SongFile.StreamFile>> pair = arg.next();
                if (pair.second.get() == null) {
                    if (!clearCacheCheckFileUsage(pair.first)) {
                        boolean deleteResult = new File(pair.first).delete();
                        log.i("Removing cache file [status " + deleteResult + "] : " + pair.first);
                    }
                    arg.remove();
                }
                return false;
            }
        });
    }

    private boolean clearCacheCheckFileUsage(String first) {
        for (P<String, WeakReference<SongFile.StreamFile>> stringWeakReferenceP : mStreamFileCache) {
            if (stringWeakReferenceP.first.equals(first) && stringWeakReferenceP.second.get() != null){
                return true;
            }
        }
        return false;
    }

    private void _scheduleCacheDownloading(SongFile.StreamFile candidateStreamFile) {
        Future future = mCacheDownloadExecutor.submit(candidateStreamFile);
        mCurrentCacheDownload = new P<>(future, candidateStreamFile);
    }


    public synchronized void streamFileQueuePriorityUpdate(Priority priority) {
        for (P<String, WeakReference<SongFile.StreamFile>> stringWeakReferenceP : mStreamFileCache) {
            SongFile.StreamFile streamFile = stringWeakReferenceP.second.get();
            if (streamFile != null){
                streamFile.priority = priority;
            }
        }
    }

    public synchronized void streamFileRelease(SongFile.StreamFile streamFile) {
        /* Nothing as working using week references */
    }

    public void streamFileDownload(SongFile.StreamFile streamFile) {
        clearCache();
        log.d("Start actual downloading "+streamFile.getFilePointer().relativePath);
        if (streamFile.state == SongFile.StreamFile.State.FINISHED){
            log.d("File already downloaded "+streamFile.getFilePointer().relativePath);
            streamFileStartNextDownloading();
            return;
        }
        streamFile.state = SongFile.StreamFile.State.STARTED;
        File cacheFile = streamFile.cacheFile;
        File cacheFileFolder = cacheFile.getParentFile();
        if (!cacheFileFolder.exists() && !cacheFileFolder.mkdirs()){
            log.w("Coulnot create folder", cacheFileFolder.getAbsolutePath());
            streamFile.state = SongFile.StreamFile.State.ERROR;
            notifyFilePreparing(streamFile, false);
            streamFileStartNextDownloading();
            return;
        }
        cacheFile.delete();
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
                    return;
                }
                output.write(data, 0, count);
            }

        } catch (Exception e) {
            log.e("Error during downloading "+streamFile.getFilePointer().relativePath, e);
            streamFile.state = SongFile.StreamFile.State.ERROR;
            streamFile.transfer.releaseInput();
            notifyFilePreparing(streamFile, false);
            streamFileStartNextDownloading();
        }

        if (output != null) try {
            output.close();
        } catch (IOException whocare) {}

        log.d("Downloading finished "+streamFile.getFilePointer().relativePath);
        streamFile.transfer.releaseInput();
        streamFile.state = SongFile.StreamFile.State.FINISHED;
        notifyFilePreparing(streamFile, true);
        streamFileStartNextDownloading();
    }

    private void notifyFilePreparing(SongFile.StreamFile streamFile, boolean successful) {
        Event.send(context, Events.FILE_PREPARED, new P<FilePointer, Boolean>(streamFile.getFilePointer(), successful));
    }

    private File asCacheFile(FilePointer filePointer) {
        File answer = new File(mCacheFolder, filePointer.source.id);
        answer = new File(answer, filePointer.relativePath);
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
