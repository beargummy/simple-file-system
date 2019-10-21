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

    public SingleFileBlockStorage(RandomAccessFile file, int blockSize, int blockCount) {
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
        if (data == null)
            throw new NullPointerException("Data block is null");
        if (index >= blockCount)
            throw new IllegalArgumentException("Block index is out of bounds");
        if (data.length > blockSize)
            throw new IllegalArgumentException("Data is bigger than block");

        file.seek(index * blockSize + offset);
        file.write(data);
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
