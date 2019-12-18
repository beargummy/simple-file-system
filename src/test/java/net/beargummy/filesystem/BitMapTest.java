package net.beargummy.filesystem;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;

public class BitMapTest {

    @Test
    public void should_allocate_nodes() {
        BitMap bitMap = new BitMap(10);

        assertThat(bitMap.numFree())
                .as("free nodes in initial size")
                .isEqualTo(10);

        assertThat(bitMap.allocate())
                .as("should allocate new node at start index")
                .isEqualTo(0);
        assertThat(bitMap.numFree())
                .as("num free should be reduced")
                .isEqualTo(9);
        assertThat(bitMap.allocate())
                .as("should allocate new node at next index")
                .isEqualTo(1);
        assertThat(bitMap.numFree())
                .as("num free should be reduced")
                .isEqualTo(8);
    }

    @Test
    public void should_serialize_and_deserialize() {
        BitMap bitMap = new BitMap(10);
        bitMap.allocate();
        assertThat(bitMap.numFree())
                .isEqualTo(9);

        ByteBuffer byteBuffer = ByteBuffer.allocate(256);
        bitMap.writeTo(byteBuffer);

        byteBuffer.rewind();
        BitMap deserialized = new BitMap(byteBuffer);

        assertThat(deserialized)
                .as("deserialized")
                .isEqualTo(bitMap);

        assertThat(deserialized.numFree())
                .as("num of free nodes")
                .isEqualTo(deserialized.numFree());

        assertThat(deserialized.size())
                .as("deserialized size")
                .isEqualTo(deserialized.size());
    }

    @Test
    public void should_not_allocate_more_than_size() {
        BitMap bitMap = new BitMap(2);
        assertThat(bitMap.allocate())
                .as("first allocation")
                .isEqualTo(0);

        assertThat(bitMap.allocate())
                .as("second allocation")
                .isEqualTo(1);

        assertThat(bitMap.allocate())
                .as("third allocation")
                .isEqualTo(-1);
    }

    @Test
    public void should_free_and_reallocate() {
        BitMap bitMap = new BitMap(2);
        assertThat(bitMap.allocate())
                .as("first allocation")
                .isEqualTo(0);

        assertThat(bitMap.allocate())
                .as("second allocation")
                .isEqualTo(1);

        bitMap.free(0);

        assertThat(bitMap.allocate())
                .as("allocation after free")
                .isEqualTo(0);
    }
}