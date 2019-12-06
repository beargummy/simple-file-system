package net.beargummy.filesystem;

import java.nio.ByteBuffer;

/**
 * Mark class as being able to be serialized to/from {@link ByteBuffer}.
 */
interface ByteBufferSerializable {

    /**
     * Write content to {@code byteBuffer}.
     *
     * @param byteBuffer byteBuffer to write content to.
     */
    void writeTo(ByteBuffer byteBuffer);

}
