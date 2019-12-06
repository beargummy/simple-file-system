package net.beargummy.filesystem;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;

public class BitMapSerializationTest {

    @Test
    public void should_serialize_and_deserialize() {
        BitMap original = new BitMap(10);
        original.allocate();
        original.allocate();
        original.allocate();
        assertThat(original.numFree())
                .as("num free")
                .isEqualTo(10 - 3);

        ByteBuffer byteBuffer = ByteBuffer.allocate(4096);

        original.writeTo(byteBuffer);

        BitMap restored = new BitMap(byteBuffer.flip());

        assertThat(restored)
                .as("restored")
                .isEqualTo(original);
    }
}