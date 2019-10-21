package net.beargummy.filesystem;

import java.io.IOException;

class DefaultFile implements File {

    private final DefaultFileSystem fs;

    String name;
    INode iNode;
    private int dataBlock;
    int size;

    public DefaultFile(DefaultFileSystem fs, String name, INode iNode, int dataBlock, int size) {
        this.fs = fs;
        this.name = name;
        this.iNode = iNode;
        this.setDataBlock(dataBlock);
        this.size = size;
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
        if (null == buffer) {
            throw new NullPointerException("data is null");
        }
        if (dataBlock == -1) {
            // todo: throw or return empty array
            return;
        }
        fs.read(this, buffer, offset);
    }

    @Override
    public void write(byte[] data) throws IOException {
        write(data, 0);
    }

    @Override
    public void write(byte[] data, int offset) throws IOException {
        if (null == data) {
            throw new NullPointerException("data is null");
        }
        if (data.length > fs.getBlockSize()) {
            throw new IllegalArgumentException("data length is greater than block size " + fs.getBlockSize());
        }
        fs.writeFile(this, data, offset);
        size += data.length;
    }

    @Override
    public int getFileSize() {
        return size;
    }

    @Override
    public String toString() {
        return "DefaultFile{" +
                "fs=" + fs +
                ", name='" + name + '\'' +
                ", iNode=" + iNode +
                ", dataBlock=" + getDataBlock() +
                ", size=" + size +
                '}';
    }

    protected int getDataBlock() {
        return dataBlock;
    }

    protected void setDataBlock(int dataBlock) {
        this.dataBlock = dataBlock;
    }
}
