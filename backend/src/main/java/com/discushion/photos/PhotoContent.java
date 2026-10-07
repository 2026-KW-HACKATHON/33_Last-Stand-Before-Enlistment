package com.discushion.photos;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import static com.discushion.photos.PhotoFailure.Reason.*;

final class PhotoContent {
    static final int MAX_BYTES = 10_000_000;
    record Verified(String mime, long bytes) {}

    static Verified inspect(InputStream input, String declaredMime) {
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
