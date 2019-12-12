package net.beargummy.filesystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class INode implements ByteBufferSerializable {

    static final int SIZE = 4 // typeTag
            + 8 // iNodeNumber
            + 4 // type
            + 8 // size
            + 8 // dataBlocksCount
            + 12 * 8 // directDataBlocks
            + 8 // indirectDataBlock
            + 8 // doubleIndirectDataBlock
            ;

    private static final int typeTag = 2096414118;

    private final DefaultFileSystem fs;

    private final long iNodeNumber;
    private final FileType type;
    private long size;

    private long dataBlocksCount;

    private final int directDataBlocksMaxCount = 12;
    // up to 12 direct data blocks
    private List<Long> directDataBlocks;

    private long indirectDataBlockNode;
    private final long indirectDataBlocksMaxCount;

    private long doubleIndirectDataBlockNode;
    private final long doubleIndirectDataBlocksMaxCount;

    public INode(DefaultFileSystem fs, long iNodeNumber, FileType fileType, long size, List<Long> dataBlocks) {
        this.fs = fs;
        this.indirectDataBlocksMaxCount = fs.getBlockSize() / 8;
        this.doubleIndirectDataBlocksMaxCount = (fs.getBlockSize() / 8) * (fs.getBlockSize() / 8);

        this.iNodeNumber = iNodeNumber;
        this.type = fileType;
        this.size = size;

        this.dataBlocksCount = dataBlocks.size();
        this.directDataBlocks = dataBlocks.stream().limit(directDataBlocksMaxCount).collect(Collectors.toList());
    }

    INode(DefaultFileSystem fs, ByteBuffer byteBuffer) {
        this.fs = fs;
        this.indirectDataBlocksMaxCount = fs.getBlockSize() / 8;
        this.doubleIndirectDataBlocksMaxCount = (fs.getBlockSize() / 8) * (fs.getBlockSize() / 8);

        if (typeTag != byteBuffer.getInt()) {
            throw new IllegalArgumentException("TypeTag mismatched");
        }
        this.iNodeNumber = byteBuffer.getLong();
        this.type = FileType.valueOf(byteBuffer.getInt());
        this.size = byteBuffer.getLong();
        this.dataBlocksCount = byteBuffer.getLong();
        int directDataBlocksCount = (int) Math.min(dataBlocksCount, directDataBlocksMaxCount);
        this.directDataBlocks = new ArrayList<>(directDataBlocksCount);
        for (int i = 0; i < directDataBlocksCount; i++) {
            directDataBlocks.add(byteBuffer.getLong());
        }
    }

    long getDataBlocksCount() {
        return dataBlocksCount;
    }

    long getBlockByIndex(long index) throws IOException {
        long currentIndex = index;
        if (currentIndex >= dataBlocksCount) {
            return -1;
        }

        if (currentIndex < directDataBlocksMaxCount) {
            return directDataBlocks.get((int) index);
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

    private long getDoubleIndirectDataBlockByIndex(long currentIndex) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        long indirectDataBlockOffset = (currentIndex / getInodesPerBlock()) * 8;
        fs.readDataBlock(buffer.array(), 0, 8, indirectDataBlockOffset, doubleIndirectDataBlockNode);
        long allocatedIndirectBlock = buffer.getLong();
        long position = (currentIndex % getInodesPerBlock()) * 8;
        fs.readDataBlock(buffer.clear().array(), 0, 8, position, allocatedIndirectBlock);

        return buffer.getLong();
    }

    private long getIndirectBlockByIndex(long currentIndex) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        long position = currentIndex * 8;
        int readBytes = fs.readDataBlock(buffer.array(), 0, 8, position, indirectDataBlockNode);
        if (readBytes != 8) {
            return -1;
        }
        return buffer.getLong();
    }

    long getOrCreateBlockByIndex(long index) throws IOException {
        if (index < dataBlocksCount) {
            return getBlockByIndex(index);
        }

        return allocate();
    }

    private long allocate() throws IOException {
        long dNode = doAllocate();
        if (dNode >= 0) {
            dataBlocksCount += 1;
        }
        return dNode;
    }

    private long doAllocate() throws IOException {
        long currentDataBlockIndex = dataBlocksCount;
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

    private long allocateDirectBlock() {
        long dataBlock = fs.allocateDNode();
        if (dataBlock < 0) {
            return -1;
        }
        directDataBlocks.add(dataBlock);
        return dataBlock;
    }

    private long allocateIndirectDataBlock(long currentDataBlockIndex) throws IOException {
        if (currentDataBlockIndex == 0 && indirectDataBlockNode <= 0) {
            long allocatedDNode = fs.allocateDNode();
            if (allocatedDNode < 0) {
                return -1;
            }
            indirectDataBlockNode = allocatedDNode;
        }
        long dataBlock = fs.allocateDNode();
        if (dataBlock < 0) {
            return -1;
        }

        ByteBuffer buffer = ByteBuffer.allocate(8).putLong(dataBlock);
        fs.writeDataBlock(buffer.array(), 0, 8, currentDataBlockIndex * 8, indirectDataBlockNode);
        return dataBlock;
    }

    private long allocateDoubleIndirectDataBlock(long currentDataBlockIndex) throws IOException {
        if (currentDataBlockIndex == 0 && doubleIndirectDataBlockNode <= 0) {
            long allocatedDNode = fs.allocateDNode();
            if (allocatedDNode < 0) {
                return -1;
            }
            this.doubleIndirectDataBlockNode = allocatedDNode;
        }

        long indirectDataBlockOffset = (currentDataBlockIndex / indirectDataBlocksMaxCount) * 8;
        long allocatedIndirectBlock;
        if (currentDataBlockIndex % indirectDataBlocksMaxCount == 0) {
            allocatedIndirectBlock = fs.allocateDNode();
            if (allocatedIndirectBlock < 0) {
                return -1;
            }
            ByteBuffer buffer = ByteBuffer.allocate(8).putLong(allocatedIndirectBlock);
            fs.writeDataBlock(buffer.array(), 0, 8, indirectDataBlockOffset, doubleIndirectDataBlockNode);
        } else {
            ByteBuffer buffer = ByteBuffer.allocate(8);
            fs.readDataBlock(buffer.array(), 0, 8, indirectDataBlockOffset, doubleIndirectDataBlockNode);
            allocatedIndirectBlock = buffer.getLong();
        }

        long dataBlock = fs.allocateDNode();
        if (dataBlock < 0) {
            return -1;
        }

        ByteBuffer allocate = ByteBuffer.allocate(8).putLong(dataBlock);
        long position = (currentDataBlockIndex % indirectDataBlocksMaxCount) * 8;
        fs.writeDataBlock(allocate.array(), 0, 8, position, allocatedIndirectBlock);

        return dataBlock;
    }

    private long getInodesPerBlock() {
        return indirectDataBlocksMaxCount;
    }

    public void writeTo(ByteBuffer byteBuffer) {
        byteBuffer
                .putInt(typeTag)
                .putLong(iNodeNumber)
                .putInt(type.getCode())
                .putLong(size)
                .putLong(dataBlocksCount);
        for (int i = 0; i < Math.min(directDataBlocksMaxCount, dataBlocksCount); i++) {
            byteBuffer.putLong(directDataBlocks.get(i));
        }
        byteBuffer.putLong(indirectDataBlockNode)
                .putLong(doubleIndirectDataBlockNode);
    }

    long getINodeNumber() {
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
