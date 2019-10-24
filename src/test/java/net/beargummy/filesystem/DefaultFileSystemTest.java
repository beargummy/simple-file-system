package net.beargummy.filesystem;

import org.assertj.core.data.Index;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.RandomAccessFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DefaultFileSystemTest {

    private FileSystem defaultFileSystem;

    @Before
    public void setUp() throws Exception {
        java.io.File file = java.io.File.createTempFile("DefaultFileSystemTest", "tmp");
        file.deleteOnExit();
        DefaultFileSystem defaultFileSystem = new DefaultFileSystem(
                1,
                new SingleFileBlockStorage(new RandomAccessFile(file, "rw"), 4 * 1024, 8)
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
    public void should_write_content_to_file_and_read_back_after_open() throws IOException {
        File originalFile = defaultFileSystem.createFile("foo");
        assertThat(originalFile.getFileSize())
                .as("file size")
                .isEqualTo(0);

        byte[] originalData = "Some data".getBytes();
        originalFile.write(originalData);

        assertThat(originalFile.getFileSize())
                .as("file size")
                .isEqualTo(originalData.length);

        File reopened = defaultFileSystem.openFile("foo");

        byte[] bytes = new byte[10];
        reopened.read(bytes);
        assertThat(bytes)
                .as("read opened content")
                .containsSequence(originalData)
                .as("non-written content")
                .contains(0, Index.atIndex(bytes.length - 1));
    }

    @Test
    public void should_throw_exception_on_open_non_existing_file() throws IOException {
        File file = defaultFileSystem.createFile("foo");

        assertThatThrownBy(() -> defaultFileSystem.openFile("bar"), "opening non-existing file")
                .as("opening non-existing file exception")
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    public void should_not_allow_create_already_created_file() throws IOException {
        File file = defaultFileSystem.createFile("foo");
        assertThatThrownBy(() -> defaultFileSystem.createFile("foo"), "calling double create")
                .as("double creation file exception")
                .isInstanceOf(IllegalArgumentException.class)
                .as("double creation file exception")
                .hasMessageContaining("foo");
    }

    @Test
    public void should_delete_file() throws IOException {
        File file = defaultFileSystem.createFile("foo");
        assertThatThrownBy(() -> defaultFileSystem.createFile("foo"), "calling double create");
        defaultFileSystem.deleteFile("foo");

        File fileAgain = defaultFileSystem.createFile("foo");
    }

    @Test
    public void should_fail_on_wrong_file_name() {
        assertThatThrownBy(() -> defaultFileSystem.createFile(null), "null file name")
                .as("null file name exception")
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> defaultFileSystem.createFile("  "), "empty file name")
                .as("empty file name exception")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void should_throw_out_of_memory_when_no_allocation_blocks() throws IOException {
        for (int i = 0; i < 3; i++) {
            File file = defaultFileSystem.createFile("foo" + i);
            byte[] data = "Some data".getBytes();
            file.write(data);
        }

        assertThatThrownBy(() -> {
            File file = defaultFileSystem.createFile("foo7");
            byte[] data = "Some data".getBytes();
            file.write(data);
        }, "calling double create");
    }
}