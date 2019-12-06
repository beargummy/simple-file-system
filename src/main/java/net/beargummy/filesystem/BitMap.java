package net.beargummy.filesystem;

import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Objects;

class BitMap implements ByteBufferSerializable {

    private final int size;
    private int numAllocated;
    private final BitSet bitSet;

    BitMap(ByteBuffer byteBuffer) {
        this.size = byteBuffer.getInt();
        this.numAllocated = byteBuffer.getInt();
        this.bitSet = BitSet.valueOf(byteBuffer);
    }

    BitMap(int size) {
        this.size = size;
        this.numAllocated = 0;
        this.bitSet = new BitSet(size);
    }

    int size() {
        return size;
    }

    int allocate() {
        if (numFree() == 0) {
            return -1;
        }
        int index = bitSet.nextClearBit(0);
        if (index >= 0) {
            bitSet.set(index);
            numAllocated += 1;
        }
        return index;
    }

    int markAllocated(int index) {
        if (bitSet.get(index)) {
            throw new IllegalArgumentException("already allocated: " + index);
        }
        bitSet.set(index);
        numAllocated += 1;
        return index;
    }

    void free(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index is out of bounds: " + index);
        }

        if (bitSet.get(index) == false)
            throw new IllegalArgumentException("already free: " + index);

        bitSet.clear(index);
        numAllocated -= 1;
    }

    int numFree() {
        return size - numAllocated;
    }

    @Override
    public void writeTo(ByteBuffer byteBuffer) {
        byteBuffer.putInt(size);
        byteBuffer.putInt(numAllocated);
        byteBuffer.put(bitSet.toByteArray());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BitMap bitMap = (BitMap) o;
        return size == bitMap.size &&
                numAllocated == bitMap.numAllocated &&
                Objects.equals(bitSet, bitMap.bitSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(size, numAllocated, bitSet);
    }

    @Override
    public String toString() {
        return "BitMap{" +
                "size=" + size +
                ", numAllocated=" + numAllocated +
                ", bitSet=" + bitSet +
                '}';
    }

}
