package net.beargummy.filesystem;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class INode {

    static final int SIZE = 16 * 4;

    private int iNodeNumber;
    private FileType type;
    private int dataBlocksCount;
    // up to 12 direct data blocks
    private List<Integer> dataBlocks;
    private int size;

    public INode(int iNodeNumber, FileType fileType, int size, List<Integer> dataBlocks) {
        this.iNodeNumber = iNodeNumber;
        this.type = fileType;
        this.dataBlocksCount = dataBlocks.size();
        this.dataBlocks = new ArrayList<>(dataBlocks);
        this.size = size;
    }

    INode(ByteBuffer byteBuffer) {
        this.type = FileType.valueOf(byteBuffer.getInt());
        this.size = byteBuffer.getInt();
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
                .putInt(type.getCode())
                .putInt(size)
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

    int getSize() {
        return size;
    }

    void setSize(int size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "INode{" +
                "iNodeNumber=" + iNodeNumber +
                ", type=" + type +
                ", dataBlocksCount=" + dataBlocksCount +
                ", dataBlocks=" + dataBlocks +
                ", size=" + size +
                '}';
    }
}
