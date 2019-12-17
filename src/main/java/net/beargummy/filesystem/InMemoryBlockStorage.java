package net.beargummy.filesystem;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simplest in-memory block storage for debugging simplification.
 */
class InMemoryBlockStorage implements BlockStorage {

    private byte[][] storage;
    private final int blocksCount;
    private final int blockSize;

    private final AtomicBoolean closed;

    InMemoryBlockStorage(int blockSize, int blocksCount) {
        this.storage = new byte[blocksCount][blockSize];
        this.blocksCount = blocksCount;
        this.blockSize = blockSize;
        closed = new AtomicBoolean(false);
    }

    @Override
    public int readBlock(long blockNumber, byte[] buffer) throws IOException {
        assertNotClosed();
        return readBlock(blockNumber, buffer, 0, blockSize, 0);
    }

    @Override
    public int readBlock(long blockNumber, byte[] buffer, int offset, int length, long position) throws IOException {
        assertNotClosed();
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
        assertNotClosed();
        writeBlock(blockNumber, buffer, 0, blockSize, 0);
    }

    @Override
    public void writeBlock(long blockNumber, byte[] buffer, int offset, int length, long position) throws IOException {
        assertNotClosed();
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
        assertNotClosed();
        return blockSize;
    }

    @Override
    public long getBlocksCount() {
        assertNotClosed();
        return blocksCount;
    }

    @Override
    public void close() throws Exception {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        storage = null; // free memory
    }

    private void assertNotClosed() {
        if (closed.get()) {
            throw new IllegalStateException("Block storage closed");
        }
    }
}
