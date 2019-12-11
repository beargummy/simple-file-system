package net.beargummy.filesystem;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultFileSystemBigFileTest {

    public static final int BLOCK_SIZE = 1024;
    private FileSystem defaultFileSystem;

    @Before
    public void setUp() throws Exception {
        java.io.File file = java.io.File.createTempFile("DefaultFileSystemTest", "tmp");
        file.deleteOnExit();
        DefaultFileSystem defaultFileSystem = new DefaultFileSystem(
                1,
                new SingleFileBlockStorage(new RandomAccessFile(file, "rw"), BLOCK_SIZE, 4096)
        );
        defaultFileSystem.initFileSystem();
        this.defaultFileSystem = defaultFileSystem;
    }

    @Test
    public void should_read_and_write_indirect_blocked_inode() throws IOException {
        File file = defaultFileSystem.createFile("foo");

        ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE);
        int bytesWritten = 0;
        for (int i = 0; i < 14; i++) {
            buffer.clear();
            for (int j = 0; j < BLOCK_SIZE / 4; j++) {
                buffer.putInt(i);
            }
            bytesWritten += file.write(buffer.array(), 0, BLOCK_SIZE, BLOCK_SIZE * i);
        }
        assertThat(bytesWritten)
                .as("bytes written")
                .isEqualTo(BLOCK_SIZE * 14);

        assertThat(file.getFileSize())
                .as("file size")
                .isEqualTo(bytesWritten);

        buffer.clear();
        int bytesRed = file.read(buffer.array(), 0, 8, BLOCK_SIZE * 13 - 4);
        assertThat(bytesRed)
                .as("bytes read")
                .isEqualTo(8);

        assertThat(buffer.getInt())
                .as("first int read")
                .isEqualTo(12);
        assertThat(buffer.getInt())
                .as("second int read")
                .isEqualTo(13);
    }

    @Test
    public void should_read_and_write_double_indirect_blocked_inode() throws IOException {
        File file = defaultFileSystem.createFile("foo");

        ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE);
        long bytesWritten = 0;
        int directINodes = 12;
        int indirectINodes = (BLOCK_SIZE / 4);
        int nodesNeeded = directINodes + 4 * indirectINodes;
        for (int i = 0; i < nodesNeeded; i++) {
            buffer.clear();
            for (int j = 0; j < BLOCK_SIZE / 4; j++) {
                buffer.putInt(i);
            }
            int written = file.write(buffer.array(), 0, BLOCK_SIZE, BLOCK_SIZE * i);
            assertThat(written)
                    .as("bytes written on write %d", i)
                    .isEqualTo(BLOCK_SIZE);
            bytesWritten += written;
        }
        assertThat(bytesWritten)
                .as("bytes written")
                .isEqualTo(BLOCK_SIZE * nodesNeeded);

        assertThat(file.getFileSize())
                .as("file size")
                .isEqualTo(bytesWritten);

        for (int i = 0; i < nodesNeeded; i++) {
            buffer.clear();
            file.read(buffer.array(), 0, 4, i * BLOCK_SIZE);
            int first = buffer.getInt();
            assertThat(first)
                    .as("first int in node %d", i)
                    .isEqualTo(i);
            buffer.clear();
            file.read(buffer.array(), 0, 4, i * BLOCK_SIZE + BLOCK_SIZE - 4);
            assertThat(buffer.getInt())
                    .as("last int in node %d", i)
                    .isEqualTo(i);
        }

        File reopened = defaultFileSystem.openFile("foo");
        assertThat(file.getFileSize())
                .as("file size")
                .isEqualTo(bytesWritten);
    }
}