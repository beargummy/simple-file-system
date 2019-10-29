package net.beargummy.filesystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class DefaultFileSystem implements FileSystem {

    private static final int SUPER_BLOCK_NUMBER = 0;
    private static final int I_NODE_BIT_MAP_BLOCK_NUMBER = 1;
    private static final int DATA_NODE_BIT_MAP_BLOCK_NUMBER = 2;
    private static final int ALWAYS_OCCUPIED_BLOCKS = 3;

    private static final String DEFAULT_PATH_SEPARATOR = "/";

    private final ReadWriteLock lock;

    private int iNodesStartIndex;
    private int dataNodesStartIndex;

    private final int numINodes;
    private final int numDNodes;

    private final BlockStorage blockStorage;

    private BitMap indexNodeBitMap;
    private BitMap dataNodeBitMap;

    private final String pathSeparator;

    private Directory rootDirectory;

    DefaultFileSystem(BlockStorage blockStorage) {
        this(1, blockStorage);
    }

    DefaultFileSystem(int blocksPerInodeRatio, BlockStorage blockStorage) {
        this(blocksPerInodeRatio, DEFAULT_PATH_SEPARATOR, blockStorage);
    }

    DefaultFileSystem(int blocksPerInodeRatio, String pathSeparator, BlockStorage blockStorage) {
        this.numINodes = blockStorage.getBlocksCount() / blocksPerInodeRatio;

        this.iNodesStartIndex = ALWAYS_OCCUPIED_BLOCKS;
        int iNodeBlocks = (int) Math.ceil((double) numINodes * INode.SIZE / blockStorage.getBlockSize());

        this.dataNodesStartIndex = iNodesStartIndex + iNodeBlocks;
        this.numDNodes = blockStorage.getBlocksCount() - dataNodesStartIndex;

        this.blockStorage = blockStorage;
        this.pathSeparator = pathSeparator;

        this.lock = new ReentrantReadWriteLock();
    }

    void initFileSystem() throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(blockStorage.getBlockSize());

        indexNodeBitMap = new BitMap(numINodes);
        int rootINodeNumber = indexNodeBitMap.allocate();
        writeBitMap(indexNodeBitMap, I_NODE_BIT_MAP_BLOCK_NUMBER);

        dataNodeBitMap = new BitMap(numDNodes);
        int rootDNodeNumber = dataNodeBitMap.allocate();
        writeBitMap(dataNodeBitMap, DATA_NODE_BIT_MAP_BLOCK_NUMBER);

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
        assertFileNameNotEmpty(name);

        lock.writeLock().lock();
        try {
            List<String> pathParts = parsePath(name);
            String fileName = pathParts.remove(pathParts.size() - 1);

            Directory current = rootDirectory;
            current = mkdirs(current, pathParts);

            if (current.containsFile(fileName)) {
                throw new IllegalArgumentException("File name already created: " + name);
            }

            int indexNodeNumber = indexNodeBitMap.allocate();
            writeBitMap(indexNodeBitMap, I_NODE_BIT_MAP_BLOCK_NUMBER);
            INode fileINode = new INode(indexNodeNumber, FileType.FILE, 0, Collections.emptyList());
            writeINode(fileINode);
            current.addFile(fileName, fileINode);

            return new DefaultFile(this, fileName, fileINode);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private List<String> parsePath(String name) {
        List<String> pathParts = new ArrayList<>(Arrays.asList(name.split(pathSeparator)));
        if (name.startsWith("/")) {
            pathParts.remove(0); // remove root directory link
        }
        return pathParts;
    }

    private Directory mkdirs(Directory current, List<String> dirs) throws IOException {
        for (String directoryName : dirs) {
            int dirINode = current.getFileINodeNumber(directoryName);
            INode directoryINode;
            if (dirINode == -1) {
                int iNodeNumber = indexNodeBitMap.allocate();
                int dNodeNumber = dataNodeBitMap.allocate();
                directoryINode = new INode(iNodeNumber, FileType.DIRECTORY, 0, Collections.singletonList(dNodeNumber));
                writeINode(directoryINode);
                current.addFile(directoryName, directoryINode);
            } else {
                directoryINode = readINode(dirINode);
            }

            current = new Directory(this, directoryINode);
        }
        writeBitMap(indexNodeBitMap, I_NODE_BIT_MAP_BLOCK_NUMBER);
        writeBitMap(dataNodeBitMap, DATA_NODE_BIT_MAP_BLOCK_NUMBER);
        return current;
    }

    @Override
    public File openFile(String name) throws IOException {
        assertFileNameNotEmpty(name);

        lock.readLock().lock();
        try {
            List<String> pathParts = parsePath(name);
            String fileName = pathParts.remove(pathParts.size() - 1);

            Directory current = getLastDirectory(pathParts);
            int fileINodeNumber = current.getFileINodeNumber(fileName);
            if (fileINodeNumber == -1) {
                throw new FileNotFoundException("File does not exist: " + name);
            }

            INode fileINode = readINode(fileINodeNumber);
            if (fileINode.getType() == FileType.DIRECTORY) {
                throw new IllegalArgumentException("File is a directory: " + name);
            }
            return new DefaultFile(this, fileName, fileINode);
        } finally {
            lock.readLock().unlock();
        }
    }

    private Directory getLastDirectory(List<String> pathParts) throws IOException {
        Directory current = rootDirectory;
        for (String directoryName : pathParts) {
            int directoryINodeNumber = current.getFileINodeNumber(directoryName);
            if (directoryINodeNumber == -1) {
                throw new FileNotFoundException("File does not exist: " + directoryName);
            }
            INode directoryINode = readINode(directoryINodeNumber);
            current = new Directory(this, directoryINode);
        }
        return current;
    }

    @Override
    public void deleteFile(String name) throws IOException {
        assertFileNameNotEmpty(name);

        lock.writeLock().lock();
        try {
            List<String> pathParts = parsePath(name);
            String fileName = pathParts.remove(pathParts.size() - 1);

            Directory current = getLastDirectory(pathParts);

            int fileINodeNumber = current.getFileINodeNumber(fileName);
            if (fileINodeNumber == -1) {
                throw new FileNotFoundException("File does not exist: " + name);
            }
            INode fileINode = readINode(fileINodeNumber);

            if (fileINode.getType() == FileType.DIRECTORY) {
                Directory directory = new Directory(this, fileINode);
                if (!directory.isEmpty()) {
                    throw new IllegalArgumentException("File is not empty directory: " + name);
                }
            }

            for (Integer dataBlock : fileINode.getDataBlocks()) {
                dataNodeBitMap.free(dataBlock);
            }
            writeBitMap(dataNodeBitMap, DATA_NODE_BIT_MAP_BLOCK_NUMBER);

            indexNodeBitMap.free(fileINodeNumber);
            writeBitMap(indexNodeBitMap, I_NODE_BIT_MAP_BLOCK_NUMBER);

            current.deleteFile(fileName);

        } finally {
            lock.writeLock().unlock();
        }
    }

    private void assertFileNameNotEmpty(String name) {
        if (name == null) {
            throw new NullPointerException("File name cannot be null");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }
        int lastIndexOf = name.lastIndexOf(pathSeparator);
        if (lastIndexOf != -1) {
            String actualName = name.substring(lastIndexOf + 1);
            if (actualName.isBlank()) {
                throw new IllegalArgumentException("File name cannot be empty");
            }
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

    private void writeBitMap(BitMap bitMap, int blockNumber) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(blockStorage.getBlockSize());
        bitMap.serialize(byteBuffer);
        blockStorage.writeBlock(byteBuffer.array(), blockNumber);
    }

    int getBlockSize() {
        return blockStorage.getBlockSize();
    }
}
