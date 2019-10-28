package net.beargummy.filesystem;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Single-file-backed implementation of {@link BlockStorage}.
 */
class SingleFileBlockStorage implements BlockStorage {

    private final RandomAccessFile file;
    private final int blockSize;
    private final int blockCount;

    SingleFileBlockStorage(RandomAccessFile file, int blockSize, int blockCount) {
        this.file = file;
        this.blockSize = blockSize;
        this.blockCount = blockCount;
    }

    @Override
    public void readBlock(byte[] buffer, int index) throws IOException {
        readBlock(buffer, index, 0);
    }

    @Override
    public void readBlock(byte[] buffer, int index, int offset) throws IOException {
        file.seek(index * blockSize + offset);
        file.read(buffer);
    }

    @Override
    public void writeBlock(byte[] data, int index) throws IOException {
        writeBlock(data, index, 0);
    }

    @Override
    public void writeBlock(byte[] data, int index, int offset) throws IOException {
        assertDataNonNull(data);
        writeBlock(data, index, offset, data.length);
    }

    @Override
    public void writeBlock(byte[] data, int index, int offset, int length) throws IOException {
        assertDataNonNull(data);
        if (index >= blockCount)
            throw new IllegalArgumentException("Block index is out of bounds");
        if (data.length > blockSize)
            throw new IllegalArgumentException("Data is greater than block");
        if (length > data.length)
            throw new IllegalArgumentException("Length is greater than data");

        file.seek(index * blockSize + offset);
        file.write(data, 0, Math.min(blockSize - (offset % blockSize), length));
    }

    private void assertDataNonNull(byte[] buffer) {
        if (buffer == null)
            throw new NullPointerException("Buffer is null");
    }

    @Override
    public int getBlockSize() {
        return blockSize;
    }

    @Override
    public int getBlocksCount() {
        return blockCount;
    }
}
