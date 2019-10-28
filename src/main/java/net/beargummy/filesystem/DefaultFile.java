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
    public void read(byte[] buffer) throws IOException {
        assertBufferNonNull(buffer);
        read(buffer, 0);
    }

    @Override
    public void read(byte[] buffer, int offset) throws IOException {
        assertBufferNonNull(buffer);
        assertPositiveOffset(offset);

        if (iNode.getDataBlocks().isEmpty()) {
            // todo: throw or return with empty array ?
            return;
        }
        fs.readINodeData(iNode, buffer, offset);
    }

    @Override
    public void write(byte[] buffer) throws IOException {
        assertBufferNonNull(buffer);
        write(buffer, 0);
    }

    @Override
    public void write(byte[] buffer, int offset) throws IOException {
        assertBufferNonNull(buffer);
        assertPositiveOffset(offset);

        fs.writeINodeData(iNode, buffer, offset);
        iNode.setSize(iNode.getSize() + buffer.length);
        fs.writeINode(iNode);
    }

    private void assertPositiveOffset(int offset) {
        if (offset < 0)
            throw new IllegalArgumentException("Offset cannot be negative");
    }

    private void assertBufferNonNull(byte[] buffer) {
        if (buffer == null)
            throw new NullPointerException("Buffer is null");
    }

    @Override
    public int getFileSize() {
        return iNode.getSize();
    }

}
