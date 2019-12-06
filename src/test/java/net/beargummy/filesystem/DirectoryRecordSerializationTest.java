package net.beargummy.filesystem;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;

public class DirectoryRecordSerializationTest {

    @Test
    public void should_serialize_and_deserialize() {
        DirectoryRecord original = new DirectoryRecord(FileType.FILE, 1, "foo");

        ByteBuffer byteBuffer = ByteBuffer.allocate(4096);

        original.writeTo(byteBuffer);

        DirectoryRecord restored = new DirectoryRecord(byteBuffer.flip());

        assertThat(restored)
                .as("restored")
                .isEqualTo(original);
    }
}