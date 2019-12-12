package net.beargummy.filesystem;

import java.nio.ByteBuffer;
import java.util.Objects;

class DirectoryRecord implements ByteBufferSerializable {
    static final int MIN_SIZE = 3 * 4 + 8;

    FileType recordType;
    int recordSize;
    long iNodeNumber;
    int nameSize;
    String name;

    DirectoryRecord(ByteBuffer byteBuffer) {
        this.recordType = FileType.valueOf(byteBuffer.getInt());
        this.recordSize = byteBuffer.getInt();
        this.iNodeNumber = byteBuffer.getLong();
        this.nameSize = byteBuffer.getInt();
        byte[] nameBytes = new byte[nameSize];
        byteBuffer.get(nameBytes);
        this.name = new String(nameBytes);
    }

    DirectoryRecord(FileType fileType, long iNodeNumber, String name) {
        this.recordType = fileType;
        this.recordSize = nameSize + MIN_SIZE;
        this.iNodeNumber = iNodeNumber;
        this.nameSize = name.getBytes().length;
        this.name = name;
    }

    @Override
    public void writeTo(ByteBuffer byteBuffer) {
        byteBuffer
                .putInt(recordType.getCode())
                .putInt(recordSize)
                .putLong(iNodeNumber)
                .putInt(nameSize);
        if (nameSize > 0) {
            byteBuffer.put(name.getBytes());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DirectoryRecord that = (DirectoryRecord) o;
        return recordSize == that.recordSize &&
                iNodeNumber == that.iNodeNumber &&
                nameSize == that.nameSize &&
                recordType == that.recordType &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recordType, recordSize, iNodeNumber, nameSize, name);
    }

    @Override
    public String toString() {
        return "DirectoryRecord{" +
                "recordType=" + recordType +
                ", recordSize=" + recordSize +
                ", iNodeNumber=" + iNodeNumber +
                ", nameSize=" + nameSize +
                ", name='" + name + '\'' +
                '}';
    }
}
