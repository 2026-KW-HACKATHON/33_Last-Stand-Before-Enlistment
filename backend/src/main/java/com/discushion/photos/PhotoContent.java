package com.discushion.photos;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import static com.discushion.photos.PhotoFailure.Reason.*;

final class PhotoContent {
    static final int MAX_BYTES = 10_000_000;
    record Verified(String mime, long bytes) {}
    // No unbounded task/thread queue if a decoder ignores interruption.
    private static final ExecutorService DECODERS=new ThreadPoolExecutor(0,2,30,TimeUnit.SECONDS,
        new SynchronousQueue<>(),task->{var thread=new Thread(task,"photo-image-check");thread.setDaemon(true);return thread;});

    static Verified inspect(InputStream input, String declaredMime) {
        if(input instanceof SupabasePhotoStorage.DeadlineImageInput bounded)
            return inspectBeforeDeadline(input,declaredMime,bounded.deadline);
        return inspectBytes(input,declaredMime);
    }
    static Verified inspectBeforeDeadline(InputStream input,String declaredMime,long deadline) {
            Future<Verified> pending=null;
            try(input) {
                long remaining=deadline-System.nanoTime();
                if(remaining<=0) throw new PhotoFailure(PHOTO_STORAGE_UNAVAILABLE);
                pending=DECODERS.submit(()->inspectBytes(input,declaredMime));
                return pending.get(remaining,TimeUnit.NANOSECONDS);
            } catch(InterruptedException failure) {
                Thread.currentThread().interrupt();throw new PhotoFailure(PHOTO_STORAGE_UNAVAILABLE);
            } catch(ExecutionException failure) {
                if(failure.getCause() instanceof PhotoFailure photo) throw photo;
                throw new PhotoFailure(PHOTO_STORAGE_UNAVAILABLE);
            } catch(IOException | TimeoutException | RejectedExecutionException failure) {
                throw new PhotoFailure(PHOTO_STORAGE_UNAVAILABLE);
            } finally {if(pending!=null && !pending.isDone())pending.cancel(true);}
    }
    private static Verified inspectBytes(InputStream input,String declaredMime) {
        try (input) {
            byte[] bytes=input.readNBytes(MAX_BYTES+1);
            if(bytes.length>MAX_BYTES) throw new PhotoFailure(PHOTO_SIZE_EXCEEDED);
            if(bytes.length==0) throw new PhotoFailure(PHOTO_FORMAT_UNSUPPORTED);
            try(var image=new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers=ImageIO.getImageReaders(image);
                if(!readers.hasNext()) throw new PhotoFailure(PHOTO_FORMAT_UNSUPPORTED);
                var reader=readers.next();
                try {
                    reader.setInput(image,true,true);
                    String format=reader.getFormatName();
                    String mime=switch(format.toLowerCase(java.util.Locale.ROOT)) {
                        case "jpeg", "jpg" -> "image/jpeg";
                        case "png" -> "image/png";
                        default -> throw new PhotoFailure(PHOTO_FORMAT_UNSUPPORTED);
                    };
                    if(!mime.equals(declaredMime)) throw new PhotoFailure(PHOTO_FORMAT_UNSUPPORTED);
                    int width=reader.getWidth(0), height=reader.getHeight(0);
                    if(width<=0 || height<=0) throw new PhotoFailure(PHOTO_FORMAT_UNSUPPORTED);
                    // Bound decoded memory without introducing a product pixel/dimension limit.
                    var parameters=reader.getDefaultReadParam();
                    parameters.setSourceSubsampling((int)Math.max(1,((long)width+1023)/1024),
                        (int)Math.max(1,((long)height+1023)/1024),0,0);
                    if(reader.read(0,parameters)==null) throw new PhotoFailure(PHOTO_FORMAT_UNSUPPORTED);
                    return new Verified(mime,bytes.length);
                } finally { reader.dispose(); }
            } catch(IOException invalidImage) { throw new PhotoFailure(PHOTO_FORMAT_UNSUPPORTED); }
        } catch(IOException error) { throw new PhotoFailure(PHOTO_STORAGE_UNAVAILABLE); }
    }
}
