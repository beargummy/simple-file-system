package net.beargummy.filesystem;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.RandomAccessFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SingleFileBlockStorageTest {

    private static final int BLOCK_SIZE = 4 * 1024; // 4Kb
    private static final int BLOCK_COUNT = 64; // 4Kb

    private BlockStorage blockStorage;

    @Before
    public void setUp() throws IOException {
        java.io.File file = java.io.File.createTempFile("SingleFileBlockStorageTest", "tmp");
        file.deleteOnExit();
        blockStorage = new SingleFileBlockStorage(
                new RandomAccessFile(file, "rw"),
                BLOCK_SIZE, BLOCK_COUNT); // 64Kb block storage
    }

    @Test
    public void should_write_to_block_and_read_back() throws IOException {
        byte[] data = {1, 2, 3};

        blockStorage.writeBlock(0, data);

        byte[] bytes = new byte[blockStorage.getBlockSize()];
        blockStorage.readBlock(0, bytes);
        assertThat(bytes)
                .as("read written data back")
                .startsWith(data);
    }

    @Test
    public void should_fail_to_write_if_index_is_incorrect() {
        assertThatThrownBy(() -> blockStorage.writeBlock(BLOCK_COUNT, new byte[]{}))
                .as("fail to write by index greater that block counts")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Block index is out of bounds");
    }

    @Test
    public void should_fail_to_write_if_data_is_bigger_than_block_size() {
        assertThatThrownBy(() -> blockStorage.writeBlock(0, new byte[BLOCK_SIZE + 1]))
                .as("fail to write data segment greater then block size")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Data is greater than block");
    }

}
