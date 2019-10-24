package net.beargummy.filesystem;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

class DirectoryData {

    private int size;
    private List<DirectoryRecord> records;

    DirectoryData(ByteBuffer byteBuffer) {
        this.size = byteBuffer.getInt();
        this.records = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.records.add(new DirectoryRecord(byteBuffer));
        }
    }

    public void serialize(ByteBuffer byteBuffer) {
        byteBuffer.putInt(size);
        records.forEach(record -> record.serialize(byteBuffer));
    }

    void addRecord(DirectoryRecord directoryRecord) {
        this.records.add(directoryRecord);
        this.size += 1;
    }

    void deleteRecord(String name) {
        this.records.removeIf(directoryRecord -> name.equals(directoryRecord.name));
        this.size -= 1;
    }

    int getFileINodeNumber(String name) {
        for (DirectoryData.DirectoryRecord r : records) {
            if (name.equals(r.name)) {
                return r.iNodeNumber;
            }
        }
        return -1;
    }

    static class DirectoryRecord {
        static final int MIN_SIZE = 4 * 4;

        FileType recordType;
        int recordSize;
        int iNodeNumber;
        int nameSize;
        String name;

        DirectoryRecord(ByteBuffer byteBuffer) {
            this.recordType = FileType.valueOf(byteBuffer.getInt());
            this.recordSize = byteBuffer.getInt();
            this.iNodeNumber = byteBuffer.getInt();
            this.nameSize = byteBuffer.getInt();
            byte[] nameBytes = new byte[nameSize];
            byteBuffer.get(nameBytes);
            this.name = new String(nameBytes);
        }

        DirectoryRecord(FileType fileType, int iNodeNumber, String name) {
            this.recordType = fileType;
            this.recordSize = nameSize + MIN_SIZE;
            this.iNodeNumber = iNodeNumber;
            this.nameSize = name.getBytes().length;
            this.name = name;
        }

        void serialize(ByteBuffer byteBuffer) {
            byteBuffer
                    .putInt(recordType.getCode())
                    .putInt(recordSize)
                    .putInt(iNodeNumber)
                    .putInt(nameSize);
            if (nameSize > 0) {
                byteBuffer.put(name.getBytes());
            }
        }

    }
}
