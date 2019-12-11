package net.beargummy.filesystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class INode implements ByteBufferSerializable {

    static final int SIZE = 4 // typeTag
            + 4 // iNodeNumber
            + 4 // type
            + 8 // size
            + 4 // dataBlocksCount
            + 12 * 4 // directDataBlocks
            + 4 // indirectDataBlock
            + 4 // doubleIndirectDataBlock
            ;

    private static final int typeTag = 2096414118;

    private final DefaultFileSystem fs;

    private final int iNodeNumber;
    private final FileType type;
    private long size;

    private int dataBlocksCount;

    private final int directDataBlocksMaxCount = 12;
    // up to 12 direct data blocks
    private List<Integer> directDataBlocks;

    private int indirectDataBlockNode;
    private final int indirectDataBlocksMaxCount;

    private int doubleIndirectDataBlockNode;
    private final int doubleIndirectDataBlocksMaxCount;

    public INode(DefaultFileSystem fs, int iNodeNumber, FileType fileType, long size, List<Integer> dataBlocks) {
        this.fs = fs;
        this.indirectDataBlocksMaxCount = fs.getBlockSize() / 4;
        this.doubleIndirectDataBlocksMaxCount = (fs.getBlockSize() / 4) * (fs.getBlockSize() / 4);

        this.iNodeNumber = iNodeNumber;
        this.type = fileType;
        this.size = size;

        this.dataBlocksCount = dataBlocks.size();
        this.directDataBlocks = dataBlocks.stream().limit(directDataBlocksMaxCount).collect(Collectors.toList());
    }

    INode(DefaultFileSystem fs, ByteBuffer byteBuffer) {
        this.fs = fs;
        this.indirectDataBlocksMaxCount = fs.getBlockSize() / 4;
        this.doubleIndirectDataBlocksMaxCount = (fs.getBlockSize() / 4) * (fs.getBlockSize() / 4);

        if (typeTag != byteBuffer.getInt()) {
            throw new IllegalArgumentException("TypeTag mismatched");
        }
        this.iNodeNumber = byteBuffer.getInt();
        this.type = FileType.valueOf(byteBuffer.getInt());
        this.size = byteBuffer.getLong();
        this.dataBlocksCount = byteBuffer.getInt();
        int directDataBlocksCount = Math.min(dataBlocksCount, directDataBlocksMaxCount);
        this.directDataBlocks = new ArrayList<>(directDataBlocksCount);
        for (int i = 0; i < directDataBlocksCount; i++) {
            directDataBlocks.add(byteBuffer.getInt());
        }
    }

    int getDataBlocksCount() {
        return dataBlocksCount;
    }

    int getBlockByIndex(int index) throws IOException {
        int currentIndex = index;
        if (currentIndex >= dataBlocksCount) {
            return -1;
        }

        if (currentIndex < directDataBlocksMaxCount) {
            return directDataBlocks.get(index);
        }

        currentIndex -= directDataBlocksMaxCount;
        if (currentIndex < indirectDataBlocksMaxCount) {
            return getIndirectBlockByIndex(currentIndex);
        }

        currentIndex -= indirectDataBlocksMaxCount;
        if (currentIndex < doubleIndirectDataBlocksMaxCount) {
            return getDoubleIndirectDataBlockByIndex(currentIndex);
        }

        return -1;
    }

    private int getDoubleIndirectDataBlockByIndex(int currentIndex) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        int indirectDataBlockOffset = (currentIndex / getInodesPerBlock()) * 4;
        fs.readDataBlock(buffer.array(), 0, 4, indirectDataBlockOffset, doubleIndirectDataBlockNode);
        int allocatedIndirectBlock = buffer.getInt();
        int position = (currentIndex % getInodesPerBlock()) * 4;
        fs.readDataBlock(buffer.clear().array(), 0, 4, position, allocatedIndirectBlock);

        return buffer.getInt();
    }

    private int getIndirectBlockByIndex(int currentIndex) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        int position = currentIndex * 4;
        int readBytes = fs.readDataBlock(buffer.array(), 0, 4, position, indirectDataBlockNode);
        if (readBytes != 4) {
            return -1;
        }
        return buffer.getInt();
    }

    int getOrCreateBlockByIndex(int index) throws IOException {
        if (index < dataBlocksCount) {
            return getBlockByIndex(index);
        }

        return allocate();
    }

    private int allocate() throws IOException {
        int dNode = doAllocate();
        if (dNode >= 0) {
            dataBlocksCount += 1;
        }
        return dNode;
    }

    private int doAllocate() throws IOException {
        int currentDataBlockIndex = dataBlocksCount;
        if (currentDataBlockIndex < directDataBlocksMaxCount) {
            return allocateDirectBlock();
        }

        currentDataBlockIndex -= directDataBlocksMaxCount;
        if (currentDataBlockIndex < indirectDataBlocksMaxCount) {
            return allocateIndirectDataBlock(currentDataBlockIndex);
        }

        currentDataBlockIndex -= indirectDataBlocksMaxCount;
        if (currentDataBlockIndex < doubleIndirectDataBlocksMaxCount) {
            return allocateDoubleIndirectDataBlock(currentDataBlockIndex);
        }

        return -1;
    }

    private int allocateDirectBlock() {
        int dataBlock = fs.allocateDNode();
        if (dataBlock < 0) {
            return -1;
        }
        directDataBlocks.add(dataBlock);
        return dataBlock;
    }

    private int allocateIndirectDataBlock(int currentDataBlockIndex) throws IOException {
        if (currentDataBlockIndex == 0 && indirectDataBlockNode <= 0) {
            int allocatedDNode = fs.allocateDNode();
            if (allocatedDNode < 0) {
                return -1;
            }
            indirectDataBlockNode = allocatedDNode;
        }
        int dataBlock = fs.allocateDNode();
        if (dataBlock < 0) {
            return -1;
        }

        ByteBuffer buffer = ByteBuffer.allocate(4).putInt(dataBlock);
        fs.writeDataBlock(buffer.array(), 0, 4, currentDataBlockIndex * 4, indirectDataBlockNode);
        return dataBlock;
    }

    private int allocateDoubleIndirectDataBlock(int currentDataBlockIndex) throws IOException {
        if (currentDataBlockIndex == 0 && doubleIndirectDataBlockNode <= 0) {
            int allocatedDNode = fs.allocateDNode();
            if (allocatedDNode < 0) {
                return -1;
            }
            this.doubleIndirectDataBlockNode = allocatedDNode;
        }

        int indirectDataBlockOffset = (currentDataBlockIndex / indirectDataBlocksMaxCount) * 4;
        int allocatedIndirectBlock;
        if (currentDataBlockIndex % indirectDataBlocksMaxCount == 0) {
            allocatedIndirectBlock = fs.allocateDNode();
            if (allocatedIndirectBlock < 0) {
                return -1;
            }
            ByteBuffer buffer = ByteBuffer.allocate(4).putInt(allocatedIndirectBlock);
            fs.writeDataBlock(buffer.array(), 0, 4, indirectDataBlockOffset, doubleIndirectDataBlockNode);
        } else {
            ByteBuffer buffer = ByteBuffer.allocate(4);
            fs.readDataBlock(buffer.array(), 0, 4, indirectDataBlockOffset, doubleIndirectDataBlockNode);
            allocatedIndirectBlock = buffer.getInt();
        }

        int dataBlock = fs.allocateDNode();
        if (dataBlock < 0) {
            return -1;
        }

        ByteBuffer allocate = ByteBuffer.allocate(4).putInt(dataBlock);
        int position = (currentDataBlockIndex % indirectDataBlocksMaxCount) * 4;
        fs.writeDataBlock(allocate.array(), 0, 4, position, allocatedIndirectBlock);

        return dataBlock;
    }

    private int getInodesPerBlock() {
        return indirectDataBlocksMaxCount;
    }

    public void writeTo(ByteBuffer byteBuffer) {
        byteBuffer
                .putInt(typeTag)
                .putInt(iNodeNumber)
                .putInt(type.getCode())
                .putLong(size)
                .putInt(dataBlocksCount);
        for (int i = 0; i < Math.min(directDataBlocksMaxCount, dataBlocksCount); i++) {
            byteBuffer.putInt(directDataBlocks.get(i));
        }
        byteBuffer.putInt(indirectDataBlockNode)
                .putInt(doubleIndirectDataBlockNode);
    }

    int getINodeNumber() {
        return iNodeNumber;
    }

    FileType getType() {
        return type;
    }

    long getSize() {
        return size;
    }

    void setSize(long size) {
        this.size = size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        INode iNode = (INode) o;
        return iNodeNumber == iNode.iNodeNumber &&
                size == iNode.size &&
                dataBlocksCount == iNode.dataBlocksCount &&
                indirectDataBlockNode == iNode.indirectDataBlockNode &&
                doubleIndirectDataBlockNode == iNode.doubleIndirectDataBlockNode &&
                Objects.equals(fs, iNode.fs) &&
                type == iNode.type &&
                Objects.equals(directDataBlocks, iNode.directDataBlocks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fs, directDataBlocksMaxCount, indirectDataBlocksMaxCount, doubleIndirectDataBlocksMaxCount, iNodeNumber, type, size, dataBlocksCount, directDataBlocks, indirectDataBlockNode, doubleIndirectDataBlockNode);
    }

    @Override
    public String toString() {
        return "INode{" +
                "fs=" + fs +
                ", directDataBlocksMaxCount=" + directDataBlocksMaxCount +
                ", indirectDataBlocksMaxCount=" + indirectDataBlocksMaxCount +
                ", doubleIndirectDataBlocksMaxCount=" + doubleIndirectDataBlocksMaxCount +
                ", iNodeNumber=" + iNodeNumber +
                ", type=" + type +
                ", size=" + size +
                ", dataBlocksCount=" + dataBlocksCount +
                ", directDataBlocks=" + directDataBlocks +
                ", indirectDataBlockNode=" + indirectDataBlockNode +
                ", doubleIndirectDataBlockNode=" + doubleIndirectDataBlockNode +
                '}';
    }
}
