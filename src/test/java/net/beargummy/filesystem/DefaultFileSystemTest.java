package net.beargummy.filesystem;

import org.assertj.core.data.Index;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.RandomAccessFile;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultFileSystemTest {

    private FileSystem defaultFileSystem;

    @Before
    public void setUp() throws Exception {
        java.io.File file = java.io.File.createTempFile("DefaultFileSystemTest", "tmp");
        file.deleteOnExit();
        DefaultFileSystem defaultFileSystem = new DefaultFileSystem(
                8,
                56,
                new SingleFileBlockStorage(new RandomAccessFile(file, "rw"), 4 * 1024, 64) // in-memory just for test sake
        );
        defaultFileSystem.initFileSystem();
        this.defaultFileSystem = defaultFileSystem;
    }

    @Test
    public void should_create_file() throws IOException {
        File file = defaultFileSystem.createFile("foo");
        assertThat(file)
                .as("file")
                .isNotNull()
                .extracting(File::getFileSize)
                .as("file size")
                .isEqualTo(0);
    }

    @Test
    public void should_write_content_to_file_and_read_back() throws IOException {
        File file = defaultFileSystem.createFile("foo");
        assertThat(file.getFileSize())
                .as("file size")
                .isEqualTo(0);

        byte[] data = "Some data".getBytes();
        file.write(data);

        assertThat(file.getFileSize())
                .as("file size")
                .isEqualTo(data.length);

        byte[] bytes = new byte[10];
        file.read(bytes);
        assertThat(bytes)
                .as("read content")
                .containsSequence(data)
                .as("non-written content")
                .contains(0, Index.atIndex(bytes.length - 1));
    }

    @Test
    public void should_delete_file() throws IOException {
        File file = defaultFileSystem.createFile("foo");
    }
}