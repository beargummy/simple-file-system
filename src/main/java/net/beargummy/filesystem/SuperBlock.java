package net.beargummy.filesystem;

import java.nio.ByteBuffer;

class SuperBlock {

    private final int rootIndexNodeOffset;

    public SuperBlock(ByteBuffer byteBuffer) {
        rootIndexNodeOffset = byteBuffer.getInt();
    }

    public SuperBlock(int rootIndexNodeOffset) {
        this.rootIndexNodeOffset = rootIndexNodeOffset;
    }

    public int getRootIndexNodeOffset() {
        return rootIndexNodeOffset;
    }

    public void serialize(ByteBuffer byteBuffer) {
        byteBuffer.putInt(rootIndexNodeOffset);
    }
}
