package net.beargummy.filesystem;

import java.nio.ByteBuffer;

class INode {

    public static final int SIZE = 4 * 4;

    int iNodeNumber;

    FileType type;
    int size;
    int block;

    public INode(int indexNodeNumber, FileType fileType, int size, int block) {
        this.iNodeNumber = indexNodeNumber;
        this.type = fileType;
        this.size = size;
        this.block = block;
    }

    public INode(ByteBuffer byteBuffer) {
        this.type = FileType.valueOf(byteBuffer.getInt());
        this.size = byteBuffer.getInt();

        this.block = byteBuffer.getInt();
    }

    public void serialize(ByteBuffer byteBuffer) {
        byteBuffer
                .putInt(type.getCode())
                .putInt(size)
                .putInt(block);
    }

    @Override
    public String toString() {
        return "INode{" +
                "iNodeNumber=" + iNodeNumber +
                ", type=" + type +
                ", size=" + size +
                ", block=" + block +
                '}';
    }
}
