package net.beargummy.filesystem;

import java.nio.ByteBuffer;

// todo: start using it or remove it :)
class SuperBlock {

    private final int rootIndexNodeOffset;

    SuperBlock(ByteBuffer byteBuffer) {
        rootIndexNodeOffset = byteBuffer.getInt();
    }

    SuperBlock(int rootIndexNodeOffset) {
        this.rootIndexNodeOffset = rootIndexNodeOffset;
    }

    int getRootIndexNodeOffset() {
        return rootIndexNodeOffset;
    }

    public void serialize(ByteBuffer byteBuffer) {
        byteBuffer.putInt(rootIndexNodeOffset);
    }
}
