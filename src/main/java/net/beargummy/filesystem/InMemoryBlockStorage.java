package net.beargummy.filesystem;

import java.io.IOException;

/**
 * Simplest in-memory block storage for debugging simplification.
 */
class InMemoryBlockStorage implements BlockStorage {

    private final byte[][] storage;
    private final int blocksCount;
    private final int blockSize;

    InMemoryBlockStorage(int blockSize, int blocksCount) {
        this.storage = new byte[blocksCount][blockSize];
        this.blocksCount = blocksCount;
        this.blockSize = blockSize;
    }

    @Override
    public int readBlock(int blockNumber, byte[] buffer) throws IOException {
        return readBlock(blockNumber, buffer, 0, blockSize, 0);
    }

    @Override
    public int readBlock(int blockNumber, byte[] buffer, int offset, int length, int position) throws IOException {
        System.arraycopy(storage[blockNumber], position, buffer, offset, length);
        return length;
    }

    @Override
    public void writeBlock(int blockNumber, byte[] buffer) throws IOException {
        writeBlock(blockNumber, buffer, 0, blockSize, 0);
    }

    @Override
    public void writeBlock(int blockNumber, byte[] buffer, int offset, int length, int position) throws IOException {
        System.arraycopy(buffer, offset, storage[blockNumber], position, length);
    }

    @Override
    public int getBlockSize() {
        return blockSize;
    }

    @Override
    public int getBlocksCount() {
        return blocksCount;
    }
}
