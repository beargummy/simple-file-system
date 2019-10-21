package net.beargummy.filesystem;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

class Directory {

    private int recordsCount;
    private List<DirectoryData> content;

    public Directory(ByteBuffer byteBuffer) {
        this.recordsCount = byteBuffer.getInt();
        this.content = new ArrayList<>(recordsCount);
        for (int i = 0; i < recordsCount; i++) {
            content.add(new DirectoryData(byteBuffer));
        }
    }

    public Directory(int recordsCount,
                     List<DirectoryData> content) {
        this.recordsCount = recordsCount;
        this.content = content;
    }

    public void addFile(String name, int indexNodeNumber) {
        // todo: reuse deleted data
        this.content.add(new DirectoryData(FileType.FILE, indexNodeNumber, name));
    }

    public int getRecordsCount() {
        return recordsCount;
    }

    public void setRecordsCount(int recordsCount) {
        this.recordsCount = recordsCount;
    }

    public List<DirectoryData> getContent() {
        return content;
    }

    public void setContent(List<DirectoryData> content) {
        this.content = content;
    }

    public void serialize(ByteBuffer byteBuffer) {
        byteBuffer.putInt(recordsCount);
        if (content == null) {
            byteBuffer.putInt(-1);
        }
        for (int i = 0; i < recordsCount; i++) {
            DirectoryData directoryData = content.get(i);
            directoryData.serialize(byteBuffer);
        }
    }

    private void serializeDirectoryData(DirectoryData data, ByteBuffer byteBuffer) {
        if (null != data) {
            data.serialize(byteBuffer);
        } else {
            byteBuffer.putInt(-1);
        }
    }

    public static class DirectoryData {

        static final int MIN_SIZE = 4 * 4;

        FileType recordType;
        int iNodeNumber;
        int recordSize;
        int nameSize;
        String name;

        public DirectoryData(ByteBuffer byteBuffer) {
            this.recordType = FileType.valueOf(byteBuffer.getInt());
            if (recordType == FileType.UNKNOWN) {
                return;
            }

            this.iNodeNumber = byteBuffer.getInt();
            this.recordSize = byteBuffer.getInt();
            this.nameSize = byteBuffer.getInt();
            byte[] nameBytes = new byte[nameSize];
            byteBuffer.get(nameBytes);
            this.name = new String(nameBytes);
        }

        public DirectoryData(FileType fileType, int iNodeNumber, String name) {
            this.recordType = fileType;
            this.iNodeNumber = iNodeNumber;
            this.nameSize = name.getBytes().length;
            this.recordSize = nameSize + MIN_SIZE;
            this.name = name;
        }

        public void serialize(ByteBuffer byteBuffer) {
            byteBuffer
                    .putInt(recordType.getCode())
                    .putInt(iNodeNumber)
                    .putInt(recordSize)
                    .putInt(nameSize);
            if (nameSize > 0) {
                byteBuffer.put(name.getBytes());
            }
        }
    }

}
