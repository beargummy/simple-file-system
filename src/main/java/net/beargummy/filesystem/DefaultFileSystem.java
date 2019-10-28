package net.beargummy.filesystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class DefaultFileSystem implements FileSystem {

    private static final int SUPER_BLOCK_NUMBER = 0;
    private static final int I_NODE_BIT_MAP_BLOCK_NUMBER = 1;
    private static final int DATA_NODE_BIT_MAP_BLOCK_NUMBER = 2;
    private static final int ALWAYS_OCCUPIED_BLOCKS = 3;

    private final ReadWriteLock lock;

    private int iNodesStartIndex;
    private int dataNodesStartIndex;

    private final int numINodes;
    private final int numDNodes;

    private final BlockStorage blockStorage;

    private BitMap indexNodeBitMap;
    private BitMap dataNodeBitMap;

    private Directory rootDirectory;

    DefaultFileSystem(BlockStorage blockStorage) {
        this(1, blockStorage);
    }

    DefaultFileSystem(int blocksPerInodeRatio, BlockStorage blockStorage) {
        this.numINodes = blockStorage.getBlocksCount() / blocksPerInodeRatio;

        this.iNodesStartIndex = ALWAYS_OCCUPIED_BLOCKS;
        int iNodeBlocks = (int) Math.ceil((double) numINodes * INode.SIZE / blockStorage.getBlockSize());

        this.dataNodesStartIndex = iNodesStartIndex + iNodeBlocks;
        this.numDNodes = blockStorage.getBlocksCount() - dataNodesStartIndex;

        this.blockStorage = blockStorage;
        this.lock = new ReentrantReadWriteLock();
    }

    void initFileSystem() throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(blockStorage.getBlockSize());

        blockStorage.writeBlock(byteBuffer.array(), SUPER_BLOCK_NUMBER);

        indexNodeBitMap = new BitMap(numINodes);
        int rootINodeNumber = indexNodeBitMap.allocate();
        indexNodeBitMap.serialize(byteBuffer.rewind());
        blockStorage.writeBlock(byteBuffer.array(), I_NODE_BIT_MAP_BLOCK_NUMBER);

        dataNodeBitMap = new BitMap(numDNodes);
        int rootDNodeNumber = dataNodeBitMap.allocate();
        dataNodeBitMap.serialize(byteBuffer.rewind());
        blockStorage.writeBlock(byteBuffer.array(), DATA_NODE_BIT_MAP_BLOCK_NUMBER);

        rootDirectory = new Directory(this, rootINodeNumber, rootDNodeNumber);
        writeINode(rootDirectory.getINode());
    }

    void restoreFileSystem() throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(blockStorage.getBlockSize());

        blockStorage.readBlock(byteBuffer.clear().array(), I_NODE_BIT_MAP_BLOCK_NUMBER);
        indexNodeBitMap = new BitMap(byteBuffer);

        blockStorage.readBlock(byteBuffer.clear().array(), DATA_NODE_BIT_MAP_BLOCK_NUMBER);
        dataNodeBitMap = new BitMap(byteBuffer);

        rootDirectory = new Directory(this, readINode(0));
    }

    @Override
    public File createFile(String name) throws IOException {
        lock.writeLock().lock();
        try {
            assertFileNameNotEmpty(name);

            if (rootDirectory.containsFile(name)) {
                throw new IllegalArgumentException("File name already created: " + name);
            }

            int indexNodeNumber = indexNodeBitMap.allocate();
            INode fileINode = new INode(indexNodeNumber, FileType.FILE, 0, new ArrayList<>());
            writeINode(fileINode);
            rootDirectory.addFile(name, indexNodeNumber);

            return new DefaultFile(this, name, fileINode);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public File openFile(String name) throws IOException {
        lock.readLock().lock();
        try {
            assertFileNameNotEmpty(name);
            assertFileExists(name);

            int fileINodeNumber = rootDirectory.getFileINodeNumber(name);
            INode fileINode = readINode(fileINodeNumber);
            return new DefaultFile(this, name, fileINode);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void deleteFile(String name) throws IOException {
        lock.writeLock().lock();
        try {
            assertFileNameNotEmpty(name);
            assertFileExists(name);

            rootDirectory.deleteFile(name);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void assertFileExists(String name) throws IOException {
        if (!rootDirectory.containsFile(name)) {
            throw new FileNotFoundException("File does not exist: " + name);
        }
    }

    private static void assertFileNameNotEmpty(String name) {
        if (name == null) {
            throw new NullPointerException("File name cannot be null");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }
    }

    INode readINode(int iNodeIndex) throws IOException {
        lock.readLock().lock();
        try {
            int iNodeBlock = ((iNodeIndex * INode.SIZE) / blockStorage.getBlockSize()) + iNodesStartIndex;
            int iNodeOffset = (iNodeIndex * INode.SIZE) % blockStorage.getBlockSize();

            ByteBuffer byteBuffer = ByteBuffer.allocate(INode.SIZE);

            blockStorage.readBlock(byteBuffer.array(), iNodeBlock, iNodeOffset);
            return new INode(byteBuffer);
        } finally {
            lock.readLock().unlock();
        }
    }

    void writeINode(INode iNode) throws IOException {
        lock.writeLock().lock();
        try {
            int iNodeBlock = ((iNode.getINodeNumber() * INode.SIZE) / blockStorage.getBlockSize()) + iNodesStartIndex;
            int iNodeOffset = (iNode.getINodeNumber() * INode.SIZE) % blockStorage.getBlockSize();
            ByteBuffer byteBuffer = ByteBuffer.allocate(INode.SIZE);
            iNode.writeTo(byteBuffer);
            blockStorage.writeBlock(byteBuffer.array(), iNodeBlock, iNodeOffset);
        } finally {
            lock.writeLock().unlock();
        }
    }

    void readINodeData(INode iNode, byte[] buffer, int offset) throws IOException {
        lock.readLock().lock();
        try {
            if (null == buffer) {
                throw new NullPointerException("Buffer is null");
            }

            if (offset / blockStorage.getBlockSize() > iNode.getDataBlocks().size()) {
                throw new IllegalArgumentException();
            }
            int blocksNeeded = (int) Math.ceil(((double) buffer.length + offset) / blockStorage.getBlockSize());

            ByteBuffer byteBuffer = ByteBuffer.allocate(blockStorage.getBlockSize());
            int blocksRead = 0;
            for (int block = offset / blockStorage.getBlockSize(); block < blocksNeeded; block++) {
                int currentOffset = blocksRead++ == 0 ? offset % Math.min(blockStorage.getBlockSize(), buffer.length) : 0;
                int currentLength = Math.min(blockStorage.getBlockSize(), buffer.length) - currentOffset % Math.min(blockStorage.getBlockSize(), buffer.length);
                int currentBlockNumber = iNode.getDataBlocks().get(block);

                blockStorage.readBlock(byteBuffer.rewind().array(), dataNodesStartIndex + currentBlockNumber, currentOffset);
                byteBuffer.get(buffer, currentOffset, currentLength);
            }

        } finally {
            lock.readLock().unlock();
        }
    }

    void writeINodeData(INode iNode, byte[] data, int offset) throws IOException {
        lock.writeLock().lock();
        try {
            if (null == data) {
                throw new NullPointerException("Data is null");
            }
            int blocksNeeded = (int) Math.ceil(((double) data.length + offset) / blockStorage.getBlockSize());
            int currentSize = iNode.getDataBlocks().size();
            if (currentSize < blocksNeeded) {
                for (int i = 0; i < blocksNeeded - currentSize; i++) {
                    int dataBlock = dataNodeBitMap.allocate();
                    if (dataBlock == -1) {
                        throw new OutOfMemoryException("Not enough space to write");
                    }
                    iNode.assignDataBlock(dataBlock);
                }
            }

            int blocksWritten = 0;
            int bytesWritten = 0;
            ByteBuffer byteBuffer = ByteBuffer.allocate(blockStorage.getBlockSize());
            for (int block = offset / blockStorage.getBlockSize(); block < blocksNeeded; block++) {
                int currentOffset = blocksWritten == 0 ? offset % Math.min(blockStorage.getBlockSize(), data.length) : 0;
                int currentLength = Math.min(blockStorage.getBlockSize(), data.length) - bytesWritten - currentOffset % Math.min(blockStorage.getBlockSize(), data.length);
                int currentBlockNumber = iNode.getDataBlocks().get(block);

                byteBuffer.clear().put(data, bytesWritten, currentLength);
                blockStorage.writeBlock(byteBuffer.array(), dataNodesStartIndex + currentBlockNumber, currentOffset, currentLength);
                blocksWritten++;
                bytesWritten += currentLength;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    int getBlockSize() {
        return blockStorage.getBlockSize();
    }
}
