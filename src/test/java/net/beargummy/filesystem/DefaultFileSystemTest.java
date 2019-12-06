package net.beargummy.filesystem;

import org.assertj.core.data.Index;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DefaultFileSystemTest {

    public static final int BLOCK_SIZE = 4 * 1024;
    private FileSystem defaultFileSystem;

    @Before
    public void setUp() throws Exception {
        java.io.File file = java.io.File.createTempFile("DefaultFileSystemTest", "tmp");
        file.deleteOnExit();
        DefaultFileSystem defaultFileSystem = new DefaultFileSystem(
                1,
                new SingleFileBlockStorage(new RandomAccessFile(file, "rw"), BLOCK_SIZE, 8)
                // new InMemoryBlockStorage(BLOCK_SIZE, 8)
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
                .isEqualTo(0L);

        assertThat(defaultFileSystem.openFile("/foo"))
                .as("reopened")
                .isNotNull();
    }

    @Test
    public void should_fail_with_wrong_path() throws IOException {
        assertThatThrownBy(() -> defaultFileSystem.createFile("//foo/bar"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void should_create_file_in_directory() throws IOException {
        assertThat(defaultFileSystem.createFile("/foo/bar"))
                .as("file")
                .isNotNull()
                .extracting(File::getFileSize)
                .as("file size")
                .isEqualTo(0L);

        assertThat(defaultFileSystem.openFile("/foo/bar"))
                .as("reopened")
                .isNotNull();
    }

    @Test
    public void should_delete_file_in_directory() throws IOException {
        assertThat(defaultFileSystem.createFile("/foo/bar"))
                .as("file")
                .isNotNull();

        assertThat(defaultFileSystem.openFile("/foo/bar"))
                .as("reopened")
                .isNotNull();

        defaultFileSystem.deleteFile("/foo/bar");

        assertThatThrownBy(() -> defaultFileSystem.openFile("/foo/bar"), "deleted file reopen")
                .as("deleted file reopen exception")
                .isInstanceOf(FileNotFoundException.class)
                .hasMessageContaining("File does not exist");
    }

    @Test
    public void should_not_allow_open_directory_as_file() throws IOException {
        defaultFileSystem.createFile("/foo/bar");

        assertThatThrownBy(() -> defaultFileSystem.openFile("/foo"))
                .as("opening directory as a file")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File is a directory");
    }

    @Test
    public void should_not_allow_delete_non_empty_directory() throws IOException {
        defaultFileSystem.createFile("/foo/bar");

        assertThatThrownBy(() -> defaultFileSystem.deleteFile("/foo"))
                .as("opening directory as a file")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void should_fail_to_create_file_with_illegal_file_name() throws IOException {
        assertThatThrownBy(() -> defaultFileSystem.createFile("/foo/ "), "blank file name in directory")
                .as("blank file name exception")
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> defaultFileSystem.createFile("/foo/"), "empty file name in directory")
                .as("empty file name exception")
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> defaultFileSystem.createFile(" "), "blank file name")
                .as("blank file file name exception")
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> defaultFileSystem.createFile(""), "empty file name")
                .as("empty file name exception")
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> defaultFileSystem.createFile("/"), "root file name")
                .as("root file name name exception")
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> defaultFileSystem.createFile("//foo"), "double-slash in start of path")
                .as("double-slash in start of path exception")
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> defaultFileSystem.createFile("/foo//bar"), "double-slash in path")
                .as("double-slash in path exception")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void should_write_content_to_file_and_read_back() throws IOException {
        File file = defaultFileSystem.createFile("foo");
        assertThat(file.getFileSize())
                .as("file size")
                .isEqualTo(0L);

        byte[] data = "Some data".getBytes();
        file.write(data);

        assertThat(file.getFileSize())
                .as("file size")
                .isEqualTo(data.length);

        byte[] bytes = new byte[BLOCK_SIZE];
        int readBytes = file.read(bytes, 0, BLOCK_SIZE);
        assertThat(readBytes)
                .as("bytes read")
                .isEqualTo(data.length);
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
                .isEqualTo(0L);

        byte[] originalData = "Some data".getBytes();
        originalFile.write(originalData);

        assertThat(originalFile.getFileSize())
                .as("file size")
                .isEqualTo(originalData.length);

        File reopened = defaultFileSystem.openFile("foo");

        byte[] bytes = new byte[BLOCK_SIZE];
        int readBytes = reopened.read(bytes, 0, BLOCK_SIZE);
        assertThat(readBytes)
                .as("bytes read")
                .isEqualTo(originalData.length);
        assertThat(bytes)
                .as("read opened content")
                .containsSequence(originalData)
                .as("non-written content")
                .contains(0, Index.atIndex(bytes.length - 1));
    }

    @Test
    public void should_write_long_content() throws IOException {
        File originalFile = defaultFileSystem.createFile("foo");
        assertThat(originalFile.getFileSize())
                .as("file size")
                .isEqualTo(0L);

        ByteBuffer byteBuffer = ByteBuffer.allocate(BLOCK_SIZE * 3);
        byte[] data = "somedata".getBytes();
        byteBuffer.put(new byte[8]);
        for (int i = 0; i < BLOCK_SIZE * 3 - 8 - 8; i += 8) {
            byteBuffer.put(data);
        }

        byte[] array = byteBuffer.array();
        int bytesWritten = originalFile.write(array, 0, BLOCK_SIZE * 3 - 8, 0L);

        assertThat(bytesWritten)
                .as("bytes written")
                .isEqualTo(BLOCK_SIZE * 3 - 8);

        assertThat(originalFile.getFileSize())
                .as("file size")
                .isEqualTo(BLOCK_SIZE * 3 - 8);

        File reopened = defaultFileSystem.openFile("foo");

        byte[] bytes = new byte[BLOCK_SIZE + 8];
        int readBytes = reopened.read(bytes);
        assertThat(readBytes)
                .as("read bytes")
                .isEqualTo(BLOCK_SIZE + 8);
        assertThat(bytes)
                .as("initial offset gap")
                .startsWith(new byte[8])
                .as("read whole opened content")
                .startsWith(ByteBuffer.allocate(data.length + 8)
                        .put(new byte[8])
                        .put(data)
                        .array())
                .endsWith(data);
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
    public void should_write_until_there_is_a_space() throws IOException {
        for (int i = 0; i < 3; i++) {
            File file = defaultFileSystem.createFile("foo" + i);
            byte[] data = "Some data".getBytes();
            file.write(data);
        }

        File file = defaultFileSystem.createFile("foo7");
        byte[] data = "Some data".getBytes();
        int bytesWritten = file.write(data);
        assertThat(bytesWritten)
                .as("bytes written when no memory")
                .isEqualTo(0);

        defaultFileSystem.deleteFile("foo0");

        bytesWritten = file.write(data);
        assertThat(bytesWritten)
                .as("bytes written when no memory")
                .isEqualTo(data.length);
        assertThat(file.getFileSize())
                .as("file size")
                .isEqualTo((long) data.length);
    }
}