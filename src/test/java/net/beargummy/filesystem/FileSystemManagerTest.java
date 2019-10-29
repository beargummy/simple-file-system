package net.beargummy.filesystem;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FileSystemManagerTest {

    @Test
    public void should_create_new_filesystem() throws Exception {
        java.io.File file = java.io.File.createTempFile("FileSystemFactoryTest", "should_create_new_filesystem");
        file.deleteOnExit();

        FileSystem fileSystem = FileSystemManager.getInstance()
                .create(file, 4 * 1024, 8);

        File foo = fileSystem.createFile("foo");

        assertThatThrownBy(() -> fileSystem.createFile("foo"), "duplicate file creation");
    }

    @Test
    public void should_restore_filesystem() throws Exception {
        java.io.File file = java.io.File.createTempFile("FileSystemFactoryTest", "should_restore_filesystem");
        file.deleteOnExit();

        FileSystem original = FileSystemManager.getInstance()
                .create(file, 4 * 1024, 8);

        File foo = original.createFile("/foo/bar");
        byte[] originalContent = "content".getBytes();
        foo.write(originalContent);

        FileSystem restored = FileSystemManager.getInstance()
                .restore(file, 4 * 1024, 8);

        assertThat(restored)
                .as("restored FS")
                .isNotSameAs(original);

        assertThatThrownBy(() -> restored.createFile("/foo/bar"), "duplicate file creation")
                .as("duplicate file exception")
                .isInstanceOf(IllegalArgumentException.class);

        byte[] restoredContent = new byte[originalContent.length + 1];
        restored.openFile("/foo/bar")
                .read(restoredContent);

        assertThat(restoredContent)
                .as("restored content")
                .startsWith(originalContent)
                .as("ends with zeros")
                .endsWith(0);
    }

}