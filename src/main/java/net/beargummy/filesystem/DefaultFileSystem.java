package net.beargummy.filesystem;

import java.io.IOException;
import java.nio.ByteBuffer;

class DefaultFileSystem implements FileSystem {

    private static final int SUPER_BLOCK_NUMBER = 0;
    private static final int I_NODE_BIT_MAP_BLOCK_NUMBER = 1;
    private static final int DATA_NODE_BIT_MAP_BLOCK_NUMBER = 2;
    private static final int ALWAYS_OCCUPIED_BLOCKS = 3;

    private int iNodesStartIndex;
    private int dataNodesStartIndex;

    private final int numINodes;
    private final int numDNodes;

    private final BlockStorage blockStorage;

    private SuperBlock superBlock;

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
    }

    void initFileSystem() throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(blockStorage.getBlockSize());

        superBlock = new SuperBlock(0);
        superBlock.serialize(byteBuffer.rewind());
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

        blockStorage.readBlock(byteBuffer.array(), SUPER_BLOCK_NUMBER);
        superBlock = new SuperBlock(byteBuffer);

        blockStorage.readBlock(byteBuffer.rewind().array(), I_NODE_BIT_MAP_BLOCK_NUMBER);
        indexNodeBitMap = new BitMap(byteBuffer);

        blockStorage.readBlock(byteBuffer.array(), DATA_NODE_BIT_MAP_BLOCK_NUMBER);
        dataNodeBitMap = new BitMap(byteBuffer);

        rootDirectory = new Directory(this, readINode(0));
    }

    @Override
    public File createFile(String name) throws IOException {
        assertFileNameNotEmpty(name);

        if (rootDirectory.containsFile(name)) {
            throw new IllegalArgumentException("File name already created: " + name);
        }

        int indexNodeNumber = indexNodeBitMap.allocate();
        INode fileINode = new INode(indexNodeNumber, FileType.FILE, 0, -1);
        writeINode(fileINode);
        rootDirectory.addFile(name, indexNodeNumber);

        return new DefaultFile(this, name, fileINode);
    }

    @Override
    public File openFile(String name) throws IOException {
        assertFileNameNotEmpty(name);
        assertFileExists(name);

        int fileINodeNumber = rootDirectory.getFileINodeNumber(name);
        INode fileINode = readINode(fileINodeNumber);
        return new DefaultFile(this, name, fileINode);
    }

    @Override
    public void deleteFile(String name) throws IOException {
        assertFileNameNotEmpty(name);
        assertFileExists(name);

        rootDirectory.deleteFile(name);
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
        int iNodeBlock = ((iNodeIndex * INode.SIZE) / blockStorage.getBlockSize()) + iNodesStartIndex;
        int iNodeOffset = (iNodeIndex * INode.SIZE) % blockStorage.getBlockSize();

        ByteBuffer byteBuffer = ByteBuffer.allocate(INode.SIZE);

        blockStorage.readBlock(byteBuffer.array(), iNodeBlock, iNodeOffset);
        return new INode(byteBuffer);
    }

    void writeINode(INode iNode) throws IOException {
        int iNodeBlock = ((iNode.getINodeNumber() * INode.SIZE) / blockStorage.getBlockSize()) + iNodesStartIndex;
        int iNodeOffset = (iNode.getINodeNumber() * INode.SIZE) % blockStorage.getBlockSize();
        ByteBuffer byteBuffer = ByteBuffer.allocate(INode.SIZE);
        iNode.writeTo(byteBuffer);
        blockStorage.writeBlock(byteBuffer.array(), iNodeBlock, iNodeOffset);
    }

    void readINodeData(INode iNode, byte[] buffer, int offset) throws IOException {
        if (null == buffer) {
            throw new NullPointerException("Buffer is null");
        }
        blockStorage.readBlock(buffer, dataNodesStartIndex + iNode.getDataBlock(), offset);
    }

    void writeINodeData(INode iNode, byte[] data, int offset) throws IOException {
        if (null == data) {
            throw new NullPointerException("Data is null");
        }
        if (data.length + offset > blockStorage.getBlockSize()) {
            throw new IllegalArgumentException("Data length is greater than block size " + blockStorage.getBlockSize());
        }
        if (iNode.getDataBlock() == -1) {
            int dataBlock = dataNodeBitMap.allocate();
            if (dataBlock == -1) {
                throw new OutOfMemoryException("Not enough space to write");
            }
            iNode.assignDataBlock(dataBlock);
        }
        blockStorage.writeBlock(data, dataNodesStartIndex + iNode.getDataBlock(), offset);
    }

    int getBlockSize() {
        return blockStorage.getBlockSize();
    }
}
