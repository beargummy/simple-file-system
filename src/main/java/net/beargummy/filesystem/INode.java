package net.beargummy.filesystem;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

class INode implements ByteBufferSerializable {

    static final int SIZE = 4 // typeTag
            + 4 // iNodeNumber
            + 4 // type
            + 8 // size
            + 4 // dataBlocksCount
            + 12 * 4 // dataBlocks
            ;

    private static final int typeTag = 2096414118;

    private final int iNodeNumber;
    private final FileType type;
    private long size;

    private int dataBlocksCount;
    // up to 12 direct data blocks
    private List<Integer> dataBlocks;

    public INode(int iNodeNumber, FileType fileType, long size, List<Integer> dataBlocks) {
        this.iNodeNumber = iNodeNumber;
        this.type = fileType;
        this.dataBlocksCount = dataBlocks.size();
        this.dataBlocks = new ArrayList<>(dataBlocks);
        this.size = size;
    }

    INode(ByteBuffer byteBuffer) {
        if (typeTag != byteBuffer.getInt()) {
            throw new IllegalArgumentException("TypeTag mismatched");
        }
        this.iNodeNumber = byteBuffer.getInt();
        this.type = FileType.valueOf(byteBuffer.getInt());
        this.size = byteBuffer.getLong();
        this.dataBlocksCount = byteBuffer.getInt();
        this.dataBlocks = new ArrayList<>(dataBlocksCount);
        for (int i = 0; i < dataBlocksCount; i++) {
            dataBlocks.add(byteBuffer.getInt());
        }
    }

    List<Integer> getDataBlocks() {
        return Collections.unmodifiableList(dataBlocks);
    }

    void assignDataBlock(int dataBlock) {
        this.dataBlocks.add(dataBlock);
        this.dataBlocksCount += 1;
    }

    public void writeTo(ByteBuffer byteBuffer) {
        byteBuffer
                .putInt(typeTag)
                .putInt(iNodeNumber)
                .putInt(type.getCode())
                .putLong(size)
                .putInt(dataBlocksCount);
        for (int i = 0; i < dataBlocksCount; i++) {
            byteBuffer.putInt(dataBlocks.get(i));
        }
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
                type == iNode.type &&
                Objects.equals(dataBlocks, iNode.dataBlocks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(iNodeNumber, type, size, dataBlocksCount, dataBlocks);
    }

    @Override
    public String toString() {
        return "INode{" +
                "iNodeNumber=" + iNodeNumber +
                ", type=" + type +
                ", size=" + size +
                ", dataBlocksCount=" + dataBlocksCount +
                ", dataBlocks=" + dataBlocks +
                '}';
    }
}
