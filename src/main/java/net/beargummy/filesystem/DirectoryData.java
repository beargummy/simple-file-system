package net.beargummy.filesystem;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class DirectoryData implements ByteBufferSerializable {

    private int size;
    private List<DirectoryRecord> records;

    DirectoryData(List<DirectoryRecord> records) {
        this.size = records.size();
        this.records = records;
    }

    DirectoryData(ByteBuffer byteBuffer) {
        this.size = byteBuffer.getInt();
        this.records = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.records.add(new DirectoryRecord(byteBuffer));
        }
    }

    @Override
    public void writeTo(ByteBuffer byteBuffer) {
        byteBuffer.putInt(size);
        records.forEach(record -> record.writeTo(byteBuffer));
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
        for (DirectoryRecord r : records) {
            if (name.equals(r.name)) {
                return r.iNodeNumber;
            }
        }
        return -1;
    }

    int getSize() {
        return size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DirectoryData that = (DirectoryData) o;
        return size == that.size &&
                Objects.equals(records, that.records);
    }

    @Override
    public int hashCode() {
        return Objects.hash(size, records);
    }

    @Override
    public String toString() {
        return "DirectoryData{" +
                "size=" + size +
                ", records=" + records +
                '}';
    }
}
