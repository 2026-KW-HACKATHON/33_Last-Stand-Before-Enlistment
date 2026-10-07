package com.discushion.photos;

import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PhotoContentTests {
    static byte[] png() throws IOException {
        var out=new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",out); return out.toByteArray();
    }
    @Test void verifiesActualImageAndActualBytesRatherThanClaim() throws Exception {
        byte[] image=png();
        var result=PhotoContent.inspect(new ByteArrayInputStream(image),"image/png");
        assertThat(result.mime()).isEqualTo("image/png"); assertThat(result.bytes()).isEqualTo(image.length);
    }
    @Test void rejectsWrongMimeFakeImageAndTruncatedImage() throws Exception {
        assertReason(()->PhotoContent.inspect(new ByteArrayInputStream(png()),"image/jpeg"),PhotoFailure.Reason.PHOTO_FORMAT_UNSUPPORTED);
        assertReason(()->PhotoContent.inspect(new ByteArrayInputStream("not-an-image".getBytes()),"image/png"),PhotoFailure.Reason.PHOTO_FORMAT_UNSUPPORTED);
        assertReason(()->PhotoContent.inspect(new ByteArrayInputStream(java.util.Arrays.copyOf(png(),20)),"image/png"),PhotoFailure.Reason.PHOTO_FORMAT_UNSUPPORTED);
    }
    @Test void limitsReadAndClosesStorageStream() {
        var read=new java.util.concurrent.atomic.AtomicInteger();
        var closed=new java.util.concurrent.atomic.AtomicBoolean();
        InputStream infinite=new InputStream() {
            public int read() {read.incrementAndGet();return 1;}
            public void close() {closed.set(true);}
        };
        assertReason(()->PhotoContent.inspect(infinite,"image/png"),PhotoFailure.Reason.PHOTO_SIZE_EXCEEDED);
        assertThat(read.get()).isEqualTo(PhotoContent.MAX_BYTES+1); assertThat(closed).isTrue();
    }
    @Test void readOutageIsNotInvalidFile() {
        InputStream broken=new InputStream(){public int read() throws IOException {throw new IOException("synthetic-secret");}};
        assertReason(()->PhotoContent.inspect(broken,"image/png"),PhotoFailure.Reason.PHOTO_STORAGE_UNAVAILABLE);
    }
    @Test void expiredNetworkBudgetDoesNotRestartForImageValidation() throws Exception {
        assertReason(()->PhotoContent.inspect(new SupabasePhotoStorage.DeadlineImageInput(png(),System.nanoTime()-1),"image/png"),PhotoFailure.Reason.PHOTO_STORAGE_UNAVAILABLE);
    }
    @Test void validationDeadlineCancelsTaskAndClosesInput() throws Exception {
        var started=new java.util.concurrent.CountDownLatch(1);
        var stopped=new java.util.concurrent.CountDownLatch(1);
        var closed=new java.util.concurrent.atomic.AtomicBoolean();
        InputStream slow=new InputStream() {
            public int read() throws IOException {
                started.countDown();
                try {Thread.sleep(10_000);return -1;}
                catch(InterruptedException error){Thread.currentThread().interrupt();throw new IOException("synthetic interruption");}
                finally {stopped.countDown();}
            }
            public void close(){closed.set(true);}
        };
        assertReason(()->PhotoContent.inspectBeforeDeadline(slow,"image/png",System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(1)),PhotoFailure.Reason.PHOTO_STORAGE_UNAVAILABLE);
        assertThat(started.getCount()).isZero();
        assertThat(stopped.await(2,java.util.concurrent.TimeUnit.SECONDS)).isTrue();assertThat(closed).isTrue();
    }
    static void assertReason(org.assertj.core.api.ThrowableAssert.ThrowingCallable operation,PhotoFailure.Reason reason) {
        assertThatThrownBy(operation).isInstanceOfSatisfying(PhotoFailure.class,error->assertThat(error.reason()).isEqualTo(reason));
    }
}
