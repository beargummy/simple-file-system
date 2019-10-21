package net.beargummy.filesystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

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

    private INode rootNode;
    private Directory rootDirectory;

    DefaultFileSystem(int numINodes, int numDNodes, BlockStorage blockStorage) throws IOException {
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
        indexNodeBitMap.serialize(byteBuffer.rewind());
        blockStorage.writeBlock(byteBuffer.array(), I_NODE_BIT_MAP_BLOCK_NUMBER);

        dataNodeBitMap = new BitMap(numDNodes);
        dataNodeBitMap.serialize(byteBuffer.rewind());
        int rootNodeDataIndex = dataNodeBitMap.allocate();
        blockStorage.writeBlock(byteBuffer.array(), DATA_NODE_BIT_MAP_BLOCK_NUMBER);

        rootNode = new INode(superBlock.getRootIndexNodeOffset(), FileType.DIRECTORY, 0, rootNodeDataIndex);
        writeINode(rootNode);

        rootDirectory = new Directory(0, new ArrayList<>());
        writeDirectory(rootDirectory);
    }

    void restoreFileSystem() throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(blockStorage.getBlockSize());

        blockStorage.readBlock(byteBuffer.array(), SUPER_BLOCK_NUMBER);
        superBlock = new SuperBlock(byteBuffer);

        blockStorage.readBlock(byteBuffer.rewind().array(), I_NODE_BIT_MAP_BLOCK_NUMBER);
        indexNodeBitMap = new BitMap(byteBuffer);

        blockStorage.readBlock(byteBuffer.array(), DATA_NODE_BIT_MAP_BLOCK_NUMBER);
        dataNodeBitMap = new BitMap(byteBuffer);

        rootNode = readINode(superBlock.getRootIndexNodeOffset());

        rootDirectory = readDirectory();
    }

    @Override
    public File createFile(String name) throws IOException {
        return create(name, 0);
    }

    public File create(String name, int size) throws IOException {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }
        if (size < 0) {
            throw new IllegalArgumentException("File size cannot be negative");
        } else if (size >= blockStorage.getBlockSize()) {
            // todo: implement multi-block files
            throw new IllegalArgumentException("File size cannot be bigger than block size");
        }

        if (rootDirectory.getContent().size() > 80) {
            throw new IllegalArgumentException("Cannot create new file in directory {}");
        }

        if (rootDirectory.getContent().stream().anyMatch(directoryData -> name.equals(directoryData.name))) {
            throw new IllegalArgumentException("File name [" + name + "] already created");
        }

        int indexNodeNumber = indexNodeBitMap.allocate();
        INode fileINode = new INode(indexNodeNumber, FileType.FILE, size, -1);
        rootDirectory.addFile(name, indexNodeNumber);
        writeINode(fileINode);
        writeDirectory(rootDirectory);

        return new DefaultFile(this, name, fileINode, -1, size);
    }

    public boolean deleteFile(String name) {
        // todo: implement
        return false;
    }

    private Directory readDirectory() {
        // todo: implement
        return null;
    }

    private void writeDirectory(Directory directory) {
        // todo: implement
    }

    private INode readINode(int iNodeIndex) throws IOException {
        int iNodeOffset = (iNodeIndex * INode.SIZE) / blockStorage.getBlockSize();
        int iNodeBlock = iNodeOffset + I_NODES_START_INDEX;

        ByteBuffer byteBuffer = ByteBuffer.allocate(INode.SIZE);

        blockStorage.readBlock(byteBuffer.array(), iNodeBlock, iNodeOffset);
        return new INode(byteBuffer);
    }

    private void writeINode(INode iNode) throws IOException {
        int iNodeOffset = (iNode.iNodeNumber * INode.SIZE) / blockStorage.getBlockSize();
        int iNodeBlock = iNodeOffset + I_NODES_START_INDEX;
        ByteBuffer byteBuffer = ByteBuffer.allocate(INode.SIZE);
        iNode.serialize(byteBuffer);
        blockStorage.writeBlock(byteBuffer.array(), iNodeBlock, iNodeOffset);
    }

    int getBlockSize() {
        return blockStorage.getBlockSize();
    }

    void writeFile(DefaultFile file, byte[] data, int offset) throws IOException {
        if (file.getDataBlock() == -1) {
            int dataBlock = dataNodeBitMap.allocate();
            if (dataBlock == -1) {
                throw new OutOfMemoryException("Not enough space to write to file " + file.name);
            }
            file.setDataBlock(dataBlock);
        }
        blockStorage.writeBlock(data, DATA_NODES_START_INDEX + file.getDataBlock(), offset);
    }

    void read(DefaultFile defaultFile, byte[] buffer, int offset) throws IOException {
        blockStorage.readBlock(buffer, DATA_NODES_START_INDEX + defaultFile.getDataBlock(), offset);
    }
}
