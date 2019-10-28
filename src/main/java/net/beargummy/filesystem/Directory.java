package net.beargummy.filesystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;

class Directory {

    private DefaultFileSystem fs;
    private INode iNode;

    Directory(DefaultFileSystem fs, int iNodeNumber, int dataBlock) {
        this.fs = fs;
        this.iNode = new INode(iNodeNumber, FileType.DIRECTORY, 0, Collections.singletonList(dataBlock));
    }

    Directory(DefaultFileSystem fs, INode iNode) {
        this.fs = fs;
        this.iNode = iNode;
    }

    INode getINode() {
        return iNode;
    }

    // todo: consider moving it to FS itself
    void addFile(String name, int indexNodeNumber) throws IOException {
        boolean alreadyExists = containsFile(name);
        if (alreadyExists) {
            throw new FileAlreadyExists("File already exists: " + name);
        }
        DirectoryData directoryData = getDirectoryData();
        directoryData.addRecord(new DirectoryData.DirectoryRecord(FileType.FILE, indexNodeNumber, name));

        ByteBuffer byteBuffer = ByteBuffer.allocate(fs.getBlockSize());
        directoryData.serialize(byteBuffer);
        fs.writeINodeData(iNode, byteBuffer.array(), 0);
    }

    // todo: consider moving it to FS itself
    void deleteFile(String name) throws IOException {
        boolean fileExists = containsFile(name);
        if (!fileExists) {
            throw new FileNotFoundException("File does not exist: " + name);
        }

        DirectoryData directoryData = getDirectoryData();
        directoryData.deleteRecord(name);

        ByteBuffer byteBuffer = ByteBuffer.allocate(fs.getBlockSize());
        directoryData.serialize(byteBuffer);
        fs.writeINodeData(iNode, byteBuffer.array(), 0);
    }

    boolean containsFile(String name) throws IOException {
        return getFileINodeNumber(name) != -1;
    }

    int getFileINodeNumber(String name) throws IOException {
        return getDirectoryData()
                .getFileINodeNumber(name);
    }

    private DirectoryData getDirectoryData() throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(fs.getBlockSize());
        fs.readINodeData(iNode, byteBuffer.array(), 0);
        return new DirectoryData(byteBuffer);
    }

    @Override
    public String toString() {
        return "Directory{" +
                "fs=" + fs +
                ", iNode=" + iNode +
                '}';
    }
}
