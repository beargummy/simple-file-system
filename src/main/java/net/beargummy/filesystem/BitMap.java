package net.beargummy.filesystem;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;

class BitMap implements ByteBufferSerializable {

    private final long size;
    private long numAllocated;
    private final BitSet[] bitSets;

    BitMap(ByteBuffer byteBuffer) {
        this.size = byteBuffer.getLong();
        this.numAllocated = byteBuffer.getLong();
        int bitSetsCount = byteBuffer.getInt();
        this.bitSets = new BitSet[bitSetsCount];
        for (int i = 0; i < bitSetsCount; i++) {
            int nextSize = byteBuffer.getInt();
            byte[] bytes = new byte[nextSize];
            byteBuffer.get(bytes);
            bitSets[i] = BitSet.valueOf(bytes);
        }
    }

    BitMap(long size) {
        this.size = size;
        this.numAllocated = 0;
        int bitSetsNeeded = getBucket(size) + 1;
        this.bitSets = new BitSet[bitSetsNeeded];
        for (int i = 0; i < bitSetsNeeded; i++) {
            int currentSize = (i + 1 == bitSetsNeeded) ? getInternalIndex(size) : Integer.MAX_VALUE;
            this.bitSets[i] = new BitSet(currentSize);
        }
    }

    long size() {
        return size;
    }

    long allocate() {
        if (numFree() == 0) {
            return -1;
        }
        for (int i = 0, bitSetsLength = bitSets.length; i < bitSetsLength; i++) {
            BitSet bitSet = bitSets[i];
            int index = bitSet.nextClearBit(0);
            if (index >= 0) {
                bitSet.set(index);
                numAllocated += 1;
                return (i + 1) * index;
            }
        }

        return -1;
    }

    long markAllocated(long index) {
        int bucket = getBucket(index);
        int internalIndex = getInternalIndex(index);
        if (bitSets[bucket].get(internalIndex)) {
            return -1;
        }
        bitSets[bucket].set(internalIndex);
        numAllocated += 1;
        return index;
    }

    void free(long index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index is out of bounds: " + index);
        }

        int bucket = getBucket(index);
        int internalIndex = getInternalIndex(index);

        if (bitSets[bucket].get(internalIndex) == false)
            throw new IllegalArgumentException("already free: " + index);

        bitSets[bucket].clear(internalIndex);
        numAllocated -= 1;
    }

    private int getInternalIndex(long index) {
        return (int) (index % Integer.MAX_VALUE);
    }

    private int getBucket(long index) {
        return (int) (index / Integer.MAX_VALUE);
    }

    long numFree() {
        return size - numAllocated;
    }

    @Override
    public void writeTo(ByteBuffer byteBuffer) {
        byteBuffer.putLong(size);
        byteBuffer.putLong(numAllocated);
        byteBuffer.putInt(bitSets.length);
        for (BitSet bitSet : bitSets) {
            byte[] bytes = bitSet.toByteArray();
            byteBuffer.putInt(bytes.length);
            byteBuffer.put(bytes);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BitMap bitMap = (BitMap) o;
        return size == bitMap.size &&
                numAllocated == bitMap.numAllocated &&
                Arrays.equals(bitSets, bitMap.bitSets);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(size, numAllocated);
        result = 31 * result + Arrays.hashCode(bitSets);
        return result;
    }

    @Override
    public String toString() {
        return "BitMap{" +
                "size=" + size +
                ", numAllocated=" + numAllocated +
                ", bitSets=" + Arrays.toString(bitSets) +
                '}';
    }
}
