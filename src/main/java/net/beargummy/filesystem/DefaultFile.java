package net.beargummy.filesystem;

import java.io.IOException;

class DefaultFile implements File {

    private final DefaultFileSystem fs;
    private INode iNode;
    private String name;

    public DefaultFile(DefaultFileSystem fs, String name, INode iNode) {
        this.fs = fs;
        this.name = name;
        this.iNode = iNode;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        assertBufferNonNull(buffer);
        return read(buffer, 0, buffer.length, 0L);
    }

    @Override
    public int read(byte[] buffer, int offset, int length, long position) throws IOException {
        assertBufferNonNull(buffer);
        assertPositiveOffset(offset);

        if (iNode.getDataBlocksCount() == 0) {
            return 0;
        }
        return fs.readINodeData(iNode, buffer, offset, length, position);
    }

    @Override
    public int write(byte[] buffer) throws IOException {
        assertBufferNonNull(buffer);
        return write(buffer, 0, buffer.length, 0);
    }

    @Override
    public int write(byte[] buffer, int offset, int length, long position) throws IOException {
        assertBufferNonNull(buffer);
        assertPositiveOffset(offset);
        assertValidLength(buffer, offset, length);
        assertValidPosition(position, iNode);

        return fs.writeINodeData(iNode, buffer, offset, length, position);
    }

    @Override
    public int append(byte[] buffer) throws IOException {
        assertBufferNonNull(buffer);
        return append(buffer, 0, buffer.length);
    }

    @Override
    public int append(byte[] buffer, int offset, int length) throws IOException {
        assertBufferNonNull(buffer);
        assertPositiveOffset(offset);
        assertValidLength(buffer, offset, length);

        return fs.writeINodeData(iNode, buffer, offset, length, iNode.getSize());
    }

    private void assertPositiveOffset(int offset) {
        if (offset < 0)
            throw new IllegalArgumentException("Offset cannot be negative");
    }

    private void assertBufferNonNull(byte[] buffer) {
        if (buffer == null)
            throw new NullPointerException("Buffer is null");
    }

    private void assertValidLength(byte[] buffer, int offset, int length) {
        if (length <= 0)
            throw new IllegalArgumentException("Length should be strictly positive number");
        if (buffer.length - offset < length)
            throw new IllegalArgumentException("Length should not be greater than buffer size plus offset");
    }

    private void assertValidPosition(long position, INode iNode) {
        if (position > iNode.getSize() + 1)
            throw new IllegalArgumentException("Position should be less or equal to file size");
    }

    @Override
    public long getFileSize() {
        return iNode.getSize();
    }

}
