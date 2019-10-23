package net.beargummy.filesystem;

import java.io.IOException;
import java.nio.ByteBuffer;

class DefaultFileSystem implements FileSystem {

    private static final int SUPER_BLOCK_NUMBER = 0;
    private static final int I_NODE_BIT_MAP_BLOCK_NUMBER = 1;
    private static final int DATA_NODE_BIT_MAP_BLOCK_NUMBER = 2;
    private static final int I_NODES_START_INDEX = 3;
    private static final int DATA_NODES_START_INDEX = 4;

    private final int numINodes;
    private final int numDNodes;

    private final BlockStorage blockStorage;

    private SuperBlock superBlock;

    private BitMap indexNodeBitMap;
    private BitMap dataNodeBitMap;

    private Directory rootDirectory;

    DefaultFileSystem(int numINodes, int numDNodes, BlockStorage blockStorage) {
        this.numINodes = numINodes;
        this.numDNodes = numDNodes;

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

        return new DefaultFile(this, name, indexNodeNumber, -1, 0);
    }

    @Override
    public void deleteFile(String name) throws IOException {
        assertFileNameNotEmpty(name);

        if (!rootDirectory.containsFile(name)) {
            throw new NoSuchFileException("File does not exist: " + name);
        }

        rootDirectory.deleteFile(name);
    }

    private static void assertFileNameNotEmpty(String name) {
        if (name == null) {
            throw new NullPointerException("File name cannot be null");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }
    }

    private INode readINode(int iNodeIndex) throws IOException {
        int iNodeOffset = (iNodeIndex * INode.SIZE) / blockStorage.getBlockSize();
        int iNodeBlock = iNodeOffset + I_NODES_START_INDEX;

        ByteBuffer byteBuffer = ByteBuffer.allocate(INode.SIZE);

        blockStorage.readBlock(byteBuffer.array(), iNodeBlock, iNodeOffset);
        return new INode(byteBuffer);
    }

    private void writeINode(INode iNode) throws IOException {
        int iNodeBlock = ((iNode.getINodeNumber() * INode.SIZE) / blockStorage.getBlockSize()) + I_NODES_START_INDEX;
        int iNodeOffset = (iNode.getINodeNumber() * INode.SIZE) % blockStorage.getBlockSize();
        ByteBuffer byteBuffer = ByteBuffer.allocate(INode.SIZE);
        iNode.writeTo(byteBuffer);
        blockStorage.writeBlock(byteBuffer.array(), iNodeBlock, iNodeOffset);
    }

    int getBlockSize() {
        return blockStorage.getBlockSize();
    }

    void readINodeData(INode iNode, byte[] buffer, int offset) throws IOException {
        if (null == buffer) {
            throw new NullPointerException("Buffer is null");
        }
        blockStorage.readBlock(buffer, DATA_NODES_START_INDEX + iNode.getDataBlock(), offset);
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
        blockStorage.writeBlock(data, DATA_NODES_START_INDEX + iNode.getDataBlock(), offset);
    }
}
