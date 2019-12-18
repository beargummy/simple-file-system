package net.beargummy.filesystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class DefaultFileSystem implements FileSystem {

    private static final long SUPER_BLOCK_NUMBER = 0;
    private static final long I_NODE_BIT_MAP_BLOCK_NUMBER = 1;
    private static final long DATA_NODE_BIT_MAP_BLOCK_NUMBER = 2;
    private static final long ALWAYS_OCCUPIED_BLOCKS = 3;

    private static final String DEFAULT_PATH_SEPARATOR = "/";

    private final ReadWriteLock lock;

    private final long numINodes;
    private final long numDNodes;

    private final BlockStorage blockStorage;

    private final PersistenceManager persistenceManager;

    private BitMap indexNodeBitMap;
    private BitMap dataNodeBitMap;

    private final String pathSeparator;

    private Directory rootDirectory;

    private AtomicBoolean closed;

    DefaultFileSystem(BlockStorage blockStorage) {
        this(1, blockStorage);
    }

    DefaultFileSystem(int blocksPerInodeRatio, BlockStorage blockStorage) {
        this(blocksPerInodeRatio, DEFAULT_PATH_SEPARATOR, blockStorage);
    }

    DefaultFileSystem(int blocksPerInodeRatio, String pathSeparator, BlockStorage blockStorage) {
        this.numINodes = blockStorage.getBlocksCount() / blocksPerInodeRatio;

        long iNodesStartIndex = ALWAYS_OCCUPIED_BLOCKS;
        long iNodeBlocks = (long) Math.ceil((double) numINodes * INode.SIZE / blockStorage.getBlockSize());

        long dataNodesStartIndex = iNodesStartIndex + iNodeBlocks;
        this.numDNodes = blockStorage.getBlocksCount() - dataNodesStartIndex;

        this.blockStorage = blockStorage;
        this.pathSeparator = pathSeparator;

        this.lock = new ReentrantReadWriteLock();

        this.persistenceManager = new PersistenceManager(blockStorage, this, iNodesStartIndex, dataNodesStartIndex);
        this.closed = new AtomicBoolean(false);
    }

    void initFileSystem() throws IOException {
        assertNotClosed();
        lock.writeLock().lock();
        try {
            indexNodeBitMap = new BitMap(numINodes);
            long rootINodeNumber = indexNodeBitMap.allocate();
            writeBitMap(indexNodeBitMap, I_NODE_BIT_MAP_BLOCK_NUMBER);

            dataNodeBitMap = new BitMap(numDNodes);
            long rootDNodeNumber = dataNodeBitMap.allocate();
            writeBitMap(dataNodeBitMap, DATA_NODE_BIT_MAP_BLOCK_NUMBER);

            rootDirectory = new Directory(this, rootINodeNumber, rootDNodeNumber);
            persistenceManager.writeINode(rootDirectory.getINode());
        } finally {
            lock.writeLock().unlock();
        }
    }

    void restoreFileSystem() throws IOException {
        assertNotClosed();
        lock.readLock().lock();
        try {
            indexNodeBitMap = persistenceManager.readBitMap(I_NODE_BIT_MAP_BLOCK_NUMBER);
            dataNodeBitMap = persistenceManager.readBitMap(DATA_NODE_BIT_MAP_BLOCK_NUMBER);

            rootDirectory = new Directory(this, persistenceManager.readINode(0));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public File createFile(String name) throws IOException {
        return runWithLock(lock.writeLock(), () -> {
            assertValidFileName(name);
            List<String> pathParts = parsePath(name);
            String fileName = pathParts.remove(pathParts.size() - 1);

            Directory current = rootDirectory;
            current = mkdirs(current, pathParts);

            if (current.containsFile(fileName)) {
                throw new IllegalArgumentException("File name already created: " + name);
            }

            long indexNodeNumber = indexNodeBitMap.allocate();
            writeBitMap(indexNodeBitMap, I_NODE_BIT_MAP_BLOCK_NUMBER);
            INode fileINode = new INode(this, indexNodeNumber, FileType.FILE, 0L, Collections.emptyList());
            persistenceManager.writeINode(fileINode);
            current.addFile(fileName, fileINode);

            return new DefaultFile(this, fileName, fileINode);
        });
    }

    private List<String> parsePath(String name) {
        List<String> pathParts = new ArrayList<>(Arrays.asList(name.split(pathSeparator)));
        if (name.startsWith("/")) {
            pathParts.remove(0); // remove root directory link
        }
        return pathParts;
    }

    private Directory mkdirs(Directory parent, List<String> dirs) throws IOException {
        Directory current = parent;
        for (String directoryName : dirs) {
            long dirINode = current.getFileINodeNumber(directoryName);
            INode directoryINode;
            if (dirINode == -1) {
                long iNodeNumber = indexNodeBitMap.allocate();
                long dNodeNumber = dataNodeBitMap.allocate();
                directoryINode = new INode(this, iNodeNumber, FileType.DIRECTORY, getBlockSize(), Collections.singletonList(dNodeNumber));
                persistenceManager.writeINode(directoryINode);
                current.addFile(directoryName, directoryINode);
            } else {
                directoryINode = persistenceManager.readINode(dirINode);
            }

            current = new Directory(this, directoryINode);
        }
        writeBitMap(indexNodeBitMap, I_NODE_BIT_MAP_BLOCK_NUMBER);
        writeBitMap(dataNodeBitMap, DATA_NODE_BIT_MAP_BLOCK_NUMBER);
        return current;
    }

    @Override
    public File openFile(String name) throws IOException {
        return runWithLock(lock.readLock(), () -> {
            assertValidFileName(name);
            List<String> pathParts = parsePath(name);
            String fileName = pathParts.remove(pathParts.size() - 1);

            Directory current = getLastDirectory(pathParts);
            long fileINodeNumber = current.getFileINodeNumber(fileName);
            if (fileINodeNumber == -1) {
                throw new FileNotFoundException("File does not exist: " + name);
            }

            INode fileINode = persistenceManager.readINode(fileINodeNumber);
            if (fileINode.getType() == FileType.DIRECTORY) {
                throw new IllegalArgumentException("File is a directory: " + name);
            }
            return new DefaultFile(this, fileName, fileINode);
        });
    }

    private Directory getLastDirectory(List<String> pathParts) throws IOException {
        Directory current = rootDirectory;
        for (String directoryName : pathParts) {
            long directoryINodeNumber = current.getFileINodeNumber(directoryName);
            if (directoryINodeNumber == -1) {
                throw new FileNotFoundException("File does not exist: " + directoryName);
            }
            INode directoryINode = persistenceManager.readINode(directoryINodeNumber);
            current = new Directory(this, directoryINode);
        }
        return current;
    }

    @Override
    public void deleteFile(String name) throws IOException {
        runWithLock(lock.writeLock(), () -> {
            assertValidFileName(name);
            List<String> pathParts = parsePath(name);
            String fileName = pathParts.remove(pathParts.size() - 1);

            Directory current = getLastDirectory(pathParts);

            long fileINodeNumber = current.getFileINodeNumber(fileName);
            if (fileINodeNumber == -1) {
                throw new FileNotFoundException("File does not exist: " + name);
            }
            INode fileINode = persistenceManager.readINode(fileINodeNumber);

            if (fileINode.getType() == FileType.DIRECTORY) {
                Directory directory = new Directory(this, fileINode);
                if (!directory.isEmpty()) {
                    throw new IllegalArgumentException("File is not empty directory: " + name);
                }
            }

            for (int dataBlockIndex = 0; dataBlockIndex < fileINode.getDataBlocksCount(); dataBlockIndex++) {
                dataNodeBitMap.free(fileINode.getBlockByIndex(dataBlockIndex));
            }
            writeBitMap(dataNodeBitMap, DATA_NODE_BIT_MAP_BLOCK_NUMBER);

            indexNodeBitMap.free(fileINodeNumber);
            writeBitMap(indexNodeBitMap, I_NODE_BIT_MAP_BLOCK_NUMBER);

            current.deleteFile(fileName);
        });
    }

    private void assertValidFileName(String name) {
        if (name == null) {
            throw new NullPointerException("File name is null");
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("File name is empty");
        }
        if (!trimmed.equals(name)) {
            throw new IllegalArgumentException("File cannot start or end with whitespace");
        }
        if (name.contains(pathSeparator + pathSeparator)) {
            throw new IllegalArgumentException("Double path separator in name");
        }
        int lastIndexOf = name.lastIndexOf(pathSeparator);
        if (lastIndexOf != -1) {
            String actualName = name.substring(lastIndexOf + 1);
            assertValidFileName(actualName);
        }
    }

    int readINodeData(INode iNode, byte[] buffer) throws IOException {
        return readINodeData(iNode, buffer, 0, buffer.length, 0L);
    }

    int readINodeData(INode iNode, byte[] buffer, int offset, int length, long position) throws IOException {
        return runWithLock(lock.readLock(), () -> persistenceManager.readINodeData(iNode, buffer, offset, length, position));
    }

    int writeINodeData(INode iNode, byte[] data) throws IOException {
        return writeINodeData(iNode, data, 0, data.length, 0);
    }

    int writeINodeData(INode iNode, byte[] data, int offset, int length, long position) throws IOException {
        return runWithLock(lock.writeLock(), () -> {
            long oldSize = iNode.getSize();
            int bytesWritten = persistenceManager.writeINodeData(iNode, data, offset, length, position);
            long newSize = iNode.getSize();
            if (newSize != oldSize) {
                persistenceManager.writeINode(iNode);
            }
            return bytesWritten;
        });
    }

    int readDataBlock(byte[] buffer, int offset, int length, long position, long block) throws IOException {
        return persistenceManager.readDataBlock(buffer, offset, length, position, block);
    }

    void writeDataBlock(byte[] buffer, int offset, int length, long position, long block) throws IOException {
        persistenceManager.writeDataBlock(buffer, offset, length, position, block);
    }

    private void writeBitMap(BitMap bitMap, long blockNumber) throws IOException {
        persistenceManager.writeBitMap(bitMap, blockNumber);
    }

    int getBlockSize() {
        return blockStorage.getBlockSize();
    }

    long allocateDNode() {
        return dataNodeBitMap.allocate();
    }

    INode readINode(long iNodeIndex) throws IOException {
        return runWithLock(lock.readLock(), () -> persistenceManager.readINode(iNodeIndex));
    }

    private <T> T runWithLock(Lock lock, Command<T> command) throws IOException {
        assertNotClosed();
        lock.lock();
        try {
            return command.execute();
        } finally {
            lock.unlock();
        }
    }

    private void runWithLock(Lock lock, VoidCommand callable) throws IOException {
        runWithLock(lock, () -> {
            callable.execute();
            return null;
        });
    }

    @Override
    public void close() throws Exception {
        lock.writeLock().lock();
        try {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            blockStorage.close();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void assertNotClosed() {
        if (closed.get()) {
            throw new IllegalStateException("File system closed");
        }
    }

    private interface Command<T> {
        T execute() throws IOException;
    }

    private interface VoidCommand {
        void execute() throws IOException;
    }
}
