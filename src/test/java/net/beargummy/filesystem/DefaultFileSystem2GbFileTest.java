package net.beargummy.filesystem;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultFileSystem2GbFileTest {

    public static final long kilobyte = 1024;
    public static final long megabyte = 1024 * 1024;
    public static final long gigabyte = megabyte * 1024;
    public static final int BLOCK_COUNT = 4096;
    private FileSystem defaultFileSystem;

    @Before
    public void setUp() throws Exception {
        java.io.File file = java.io.File.createTempFile("DefaultFileSystemTest", "tmp");
        file.deleteOnExit();
        DefaultFileSystem defaultFileSystem = new DefaultFileSystem(
                1,
                new SingleFileBlockStorage(new RandomAccessFile(file, "rw"), (int) (megabyte), BLOCK_COUNT)
        );
        defaultFileSystem.initFileSystem();
        this.defaultFileSystem = defaultFileSystem;
    }

    @Test
    @Ignore
    public void should_append_more_than_2_gigs() throws IOException {
        File file = defaultFileSystem.createFile("foo");

        ByteBuffer writeBuffer = ByteBuffer.allocate((int) megabyte);
        ByteBuffer readBuffer = ByteBuffer.allocate((int) megabyte);
        long bytesWritten = 0;
        for (long i = 0; i < 3 * 1024; i++) {
            writeBuffer.clear();
            writeBuffer.putLong(i);
            int written = file.append(writeBuffer.array(), 0, (int) megabyte);
            bytesWritten += written;
            assertThat(written)
                    .as("bytes written on %d Mb", i)
                    .isEqualTo((int) megabyte);

            int readBytes = file.read(readBuffer.clear().array(), bytesWritten - megabyte);
            assertThat(readBytes)
                    .as("bytes read back on %d Mb", i)
                    .isEqualTo((int) megabyte);

            assertThat(readBuffer.getLong())
                    .as("read content on %d Mb", i)
                    .isEqualTo(i);
        }

        assertThat(bytesWritten)
                .as("bytes written")
                .isEqualTo(3 * gigabyte);

        assertThat(file.getFileSize())
                .as("file size")
                .isEqualTo(bytesWritten);

        byte[] bytes = readBuffer.clear().array();

        int readBytes = file.read(bytes, 3 * gigabyte - 10 * megabyte);
        assertThat(readBytes)
                .as("bytes read")
                .isEqualTo(megabyte);

        assertThat(bytes)
                .as("read content at beggining")
                .containsSequence(ByteBuffer.allocate(8).putLong(3062).array());

        readBytes = file.read(bytes, 2037L * 1024 * 1024);
        assertThat(readBytes)
                .as("bytes read")
                .isEqualTo(megabyte);

        assertThat(ByteBuffer.wrap(bytes).getLong())
                .as("read content")
                .isEqualTo(2037L);

    }
}