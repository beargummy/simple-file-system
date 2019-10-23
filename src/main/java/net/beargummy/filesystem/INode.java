package net.beargummy.filesystem;

import java.nio.ByteBuffer;

class INode {

    static final int SIZE = 4 * 4;

    private int iNodeNumber;
    private FileType type;
    private int dataBlock;
    private int size;

    INode(int indexNodeNumber, FileType fileType, int size, int block) {
        this.iNodeNumber = indexNodeNumber;
        this.type = fileType;
        this.dataBlock = block;
        this.size = size;
    }

    INode(ByteBuffer byteBuffer) {
        this.type = FileType.valueOf(byteBuffer.getInt());
        this.size = byteBuffer.getInt();
        this.dataBlock = byteBuffer.getInt();
    }

    int getDataBlock() {
        return dataBlock;
    }

    void assignDataBlock(int dataBlock) {
        this.dataBlock = dataBlock;
    }

    public void writeTo(ByteBuffer byteBuffer) {
        byteBuffer
                .putInt(type.getCode())
                .putInt(size)
                .putInt(dataBlock);
    }

    int getINodeNumber() {
        return iNodeNumber;
    }

    void setINodeNumber(int iNodeNumber) {
        this.iNodeNumber = iNodeNumber;
    }

    FileType getType() {
        return type;
    }

    void setType(FileType type) {
        this.type = type;
    }

    void setDataBlock(int dataBlock) {
        this.dataBlock = dataBlock;
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
                ", size=" + size +
                ", block=" + dataBlock +
                '}';
    }

}
