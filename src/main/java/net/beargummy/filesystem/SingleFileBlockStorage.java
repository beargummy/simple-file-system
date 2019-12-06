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
    public int readBlock(int blockNumber, byte[] buffer) throws IOException {
        return readBlock(blockNumber, buffer, 0, buffer.length, 0);
    }

    @Override
    public int readBlock(int blockNumber, byte[] buffer, int offset, int length, int position) throws IOException {
        file.seek(blockNumber * blockSize + position);
        return file.read(buffer, 0, length);
    }

    @Override
    public void writeBlock(int blockNumber, byte[] buffer) throws IOException {
        writeBlock(blockNumber, buffer, 0, buffer.length, 0);
    }

    @Override
    public void writeBlock(int blockNumber, byte[] buffer, int offset, int length, int position) throws IOException {
        assertDataNonNull(buffer);
        assertBlockNumberValid(blockNumber);
        assertOffsetValid(buffer, offset, length, position);

        file.seek(blockNumber * blockSize + position);
        file.write(buffer, offset, length);
    }

    private void assertBlockNumberValid(int blockNumber) {
        if (blockNumber >= blockCount)
            throw new IllegalArgumentException("Block index is out of bounds");
    }

    private void assertOffsetValid(byte[] buffer, int offset, int length, int position) {
        if (length + position > blockSize)
            throw new IllegalArgumentException("Data is greater than block");
        if (offset < 0)
            throw new IllegalArgumentException("Offset should be strictly positive");
        if (offset > buffer.length)
            throw new IllegalArgumentException("Offset should be less than buffer length");
    }

    private void assertLength(byte[] buffer, int offset, int length) {
        if (length - offset > buffer.length)
            throw new IllegalArgumentException("Length is greater than buffer size");
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
