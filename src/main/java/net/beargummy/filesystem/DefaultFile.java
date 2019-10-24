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
        read(buffer, 0);
    }

    @Override
    public void read(byte[] buffer, int offset) throws IOException {
        if (iNode.getDataBlock() == -1) {
            // todo: throw or return with empty array ?
            return;
        }
        fs.readINodeData(iNode, buffer, offset);
    }

    @Override
    public void write(byte[] data) throws IOException {
        write(data, 0);
    }

    @Override
    public void write(byte[] data, int offset) throws IOException {
        fs.writeINodeData(iNode, data, offset);
        iNode.setSize(iNode.getSize() + data.length);
        fs.writeINode(iNode);
    }

    @Override
    public int getFileSize() {
        return iNode.getSize();
    }

}
