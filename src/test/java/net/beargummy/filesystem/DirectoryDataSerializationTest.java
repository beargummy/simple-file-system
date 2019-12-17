package net.beargummy.filesystem;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class DirectoryDataSerializationTest {

    @Test
    public void should_serialize_and_deserialize() {
        DirectoryData original = new DirectoryData(Arrays.asList(
                new DirectoryRecord(
                        FileType.FILE, 1, "foo"
                ),
                new DirectoryRecord(
                        FileType.FILE, 2, "bar"
                )
        ));

        ByteBuffer byteBuffer = ByteBuffer.allocate(4096);

        original.writeTo(byteBuffer);

        byteBuffer.flip();
        DirectoryData restored = new DirectoryData(byteBuffer);

        assertThat(restored)
                .as("restored")
                .isEqualTo(original);
    }
}