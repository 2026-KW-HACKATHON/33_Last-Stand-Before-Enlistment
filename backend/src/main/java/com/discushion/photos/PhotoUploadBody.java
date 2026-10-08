package com.discushion.photos;

import java.io.InputStream;
import java.util.concurrent.*;
import static com.discushion.photos.PhotoFailure.Reason.*;

/** Bounds raw request bytes, receiving time and threads even if a stream ignores interruption. */
final class PhotoUploadBody {
    private static final ExecutorService READERS=new ThreadPoolExecutor(0,2,30,TimeUnit.SECONDS,
        new SynchronousQueue<>(),task->{var thread=new Thread(task,"photo-upload-body");thread.setDaemon(true);return thread;});
    static byte[] read(InputStream input,long deadline) {
        Future<byte[]> pending=null;
        try(input) {
            long remaining=deadline-System.nanoTime();
            if(remaining<=0) throw new PhotoFailure(PHOTO_STORAGE_UNAVAILABLE);
            pending=READERS.submit(()->input.readNBytes(PhotoContent.MAX_BYTES+1));
            byte[] bytes=pending.get(remaining,TimeUnit.NANOSECONDS);
            if(bytes.length>PhotoContent.MAX_BYTES) throw new PhotoFailure(PHOTO_SIZE_EXCEEDED);
            return bytes;
        } catch(InterruptedException failure) {
            Thread.currentThread().interrupt();throw new PhotoFailure(PHOTO_STORAGE_UNAVAILABLE);
        } catch(Exception failure) {
            if(failure instanceof PhotoFailure photo) throw photo;
            throw new PhotoFailure(PHOTO_STORAGE_UNAVAILABLE);
        } finally {if(pending!=null && !pending.isDone()) pending.cancel(true);}
    }
}
