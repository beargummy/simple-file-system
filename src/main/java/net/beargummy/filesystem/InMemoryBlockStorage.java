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
    public int readBlock(long blockNumber, byte[] buffer) throws IOException {
        return readBlock(blockNumber, buffer, 0, blockSize, 0);
    }

    @Override
    public int readBlock(long blockNumber, byte[] buffer, int offset, int length, long position) throws IOException {
        if (blockNumber > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("huge block numbers unsupported");
        }
        if (position > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("huge position unsupported");
        }
        System.arraycopy(storage[(int) blockNumber], (int) position, buffer, offset, length);
        return length;
    }

    @Override
    public void writeBlock(long blockNumber, byte[] buffer) throws IOException {
        writeBlock(blockNumber, buffer, 0, blockSize, 0);
    }

    @Override
    public void writeBlock(long blockNumber, byte[] buffer, int offset, int length, long position) throws IOException {
        if (blockNumber > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("huge block numbers unsupported");
        }
        if (position > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("huge position unsupported");
        }
        System.arraycopy(buffer, offset, storage[(int) blockNumber], (int) position, length);
    }

    @Override
    public int getBlockSize() {
        return blockSize;
    }

    @Override
    public long getBlocksCount() {
        return blocksCount;
    }
}
