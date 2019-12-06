package net.beargummy.filesystem;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class PersistenceManagerTest {

    public static final int BLOCK_SIZE = 128;
    public static final int I_NODES_START_INDEX = 10;
    public static final int DATA_NODES_START_INDEX = 20;

    private DefaultFileSystem fileSystem;
    private BlockStorage blockStorage;

    private PersistenceManager persistenceManager;

    @Before
    public void setUp() throws Exception {
        blockStorage = mock(BlockStorage.class);
        when(blockStorage.getBlocksCount()).thenReturn(64);
        when(blockStorage.getBlockSize()).thenReturn(BLOCK_SIZE);

        fileSystem = mock(DefaultFileSystem.class);
        AtomicInteger nextBlock = new AtomicInteger();
        when(fileSystem.allocateDNode())
                .thenAnswer(inv -> nextBlock.incrementAndGet());

        when(blockStorage.readBlock(anyInt(), any(byte[].class), anyInt(), anyInt(), anyInt()))
                .thenAnswer(invocation -> invocation.getArgument(3));

        persistenceManager = new PersistenceManager(
                blockStorage,
                fileSystem,
                I_NODES_START_INDEX,
                DATA_NODES_START_INDEX
        );
    }

    @Test
    public void should_rewrite_full_block() throws IOException {
        persistenceManager.writeINodeData(
                new INode(1, FileType.DIRECTORY, BLOCK_SIZE, Collections.singletonList(0)),
                new byte[BLOCK_SIZE], 0, BLOCK_SIZE, 0L
        );

        verify(blockStorage, times(1)).writeBlock(eq(DATA_NODES_START_INDEX), any(), eq(0), eq(BLOCK_SIZE), eq(0));
    }

    @Test
    public void should_partially_rewrite_and_append() throws IOException {
        persistenceManager.writeINodeData(
                new INode(1, FileType.FILE, 32, Collections.singletonList(0)),
                new byte[32], 0, 16, 24L
        );

        verify(blockStorage, times(1)).writeBlock(eq(DATA_NODES_START_INDEX), any(byte[].class), eq(0), eq(16), eq(24));
    }

    @Test
    public void should_partially_rewrite_first_block() throws IOException {
        persistenceManager.writeINodeData(
                new INode(1, FileType.FILE, 32, Collections.singletonList(0)),
                new byte[16], 0, 16, 8L
        );

        verify(blockStorage, times(1)).writeBlock(eq(DATA_NODES_START_INDEX), any(byte[].class), eq(0), eq(16), eq(8));
    }

    @Test
    public void should_partially_rewrite_last_block() throws IOException {
        persistenceManager.writeINodeData(
                new INode(1, FileType.FILE, BLOCK_SIZE + 32, Arrays.asList(0, 1)),
                new byte[16], 0, 16, BLOCK_SIZE + 8
        );

        verify(blockStorage, times(1)).writeBlock(eq(DATA_NODES_START_INDEX + 1), any(byte[].class), eq(0), eq(16), eq(8));
    }

    @Test
    public void should_partially_middle_last_block() throws IOException {
        persistenceManager.writeINodeData(
                new INode(1, FileType.FILE, (BLOCK_SIZE * 2) + 32, Arrays.asList(0, 1, 2)),
                new byte[16], 0, 16, BLOCK_SIZE + 8
        );

        verify(blockStorage, times(1)).writeBlock(eq(DATA_NODES_START_INDEX + 1), any(byte[].class), eq(0), eq(16), eq(8));
    }

    @Test
    public void should_append_to_the_same_block() throws IOException {
        persistenceManager.writeINodeData(
                new INode(1, FileType.FILE, 32, Collections.singletonList(0)),
                new byte[16], 0, 16, 32L
        );

        verify(blockStorage, times(1)).writeBlock(eq(DATA_NODES_START_INDEX), any(byte[].class), eq(0), eq(16), eq(32));
    }

    @Test
    public void should_append_to_the_next_block() throws IOException {
        persistenceManager.writeINodeData(
                new INode(1, FileType.FILE, BLOCK_SIZE, Collections.singletonList(0)),
                new byte[16], 0, 16, BLOCK_SIZE
        );

        verify(blockStorage, times(1)).writeBlock(eq(DATA_NODES_START_INDEX + 1), any(byte[].class), eq(0), eq(16), eq(0));
    }

    @Test
    public void should_append_to_current_and_next_blocks() throws IOException {
        persistenceManager.writeINodeData(
                new INode(1, FileType.FILE, BLOCK_SIZE - 8, Collections.singletonList(0)),
                new byte[16], 0, 16, BLOCK_SIZE - 8
        );

        verify(blockStorage, times(1)).writeBlock(eq(DATA_NODES_START_INDEX), any(byte[].class), eq(0), eq(8), eq(BLOCK_SIZE - 8));
        verify(blockStorage, times(1)).writeBlock(eq(DATA_NODES_START_INDEX + 1), any(byte[].class), eq(0), eq(8), eq(0));
    }

    @Test
    public void should_read_full_block() throws IOException {
        persistenceManager.readINodeData(
                new INode(1, FileType.DIRECTORY, BLOCK_SIZE, Collections.singletonList(0)),
                new byte[BLOCK_SIZE], 0, BLOCK_SIZE, 0L
        );

        verify(blockStorage, times(1)).readBlock(eq(DATA_NODES_START_INDEX), any(), eq(0), eq(BLOCK_SIZE), eq(0));
    }

    @Test
    public void should_read_part_of_block_up_to_max_data() throws IOException {
        int bytesRead = persistenceManager.readINodeData(
                new INode(1, FileType.FILE, 32, Collections.singletonList(0)),
                new byte[32], 0, 16, 24L
        );

        assertThat(bytesRead)
                .as("bytes read")
                .isEqualTo(8);
        verify(blockStorage, times(1)).readBlock(eq(DATA_NODES_START_INDEX), any(byte[].class), eq(0), eq(8), eq(24));
    }

    @Test
    public void should_read_part_of_first_block() throws IOException {
        int bytesRead = persistenceManager.readINodeData(
                new INode(1, FileType.FILE, 32, Collections.singletonList(0)),
                new byte[32], 0, 16, 8L
        );

        assertThat(bytesRead)
                .as("bytes read")
                .isEqualTo(16);
        verify(blockStorage, times(1)).readBlock(eq(DATA_NODES_START_INDEX), any(byte[].class), eq(0), eq(16), eq(8));
    }

    @Test
    public void should_read_part_of_last_block() throws IOException {
        int bytesRead = persistenceManager.readINodeData(
                new INode(1, FileType.FILE, BLOCK_SIZE * 2 + 32, Arrays.asList(0, 1, 2)),
                new byte[32], 0, 16, BLOCK_SIZE * 2 + 8
        );

        assertThat(bytesRead)
                .as("bytes read")
                .isEqualTo(16);
        verify(blockStorage, times(1)).readBlock(eq(DATA_NODES_START_INDEX + 2), any(byte[].class), eq(0), eq(16), eq(8));
    }

    @Test
    public void should_read_part_of_current_and_last_block() throws IOException {
        int bytesRead = persistenceManager.readINodeData(
                new INode(1, FileType.FILE, BLOCK_SIZE + 32, Arrays.asList(0, 1)),
                new byte[32], 0, 16, BLOCK_SIZE - 8
        );

        assertThat(bytesRead)
                .as("bytes read")
                .isEqualTo(16);
        verify(blockStorage, times(1)).readBlock(eq(DATA_NODES_START_INDEX), any(byte[].class), eq(0), eq(8), eq(BLOCK_SIZE - 8));
        verify(blockStorage, times(1)).readBlock(eq(DATA_NODES_START_INDEX + 1), any(byte[].class), eq(0), eq(8), eq(0));
    }

    @Test
    public void should_fail_to_read_unexisting_content() throws IOException {
        int bytesRead = persistenceManager.readINodeData(
                new INode(1, FileType.FILE, 32, Collections.singletonList(0)),
                new byte[32], 0, 16, 32L
        );
        assertThat(bytesRead)
                .as("bytes read")
                .isEqualTo(0);

        verify(blockStorage, never()).readBlock(anyInt(), any(byte[].class), anyInt(), anyInt(), anyInt());
    }

    @Test
    public void should_fail_to_read_wrong_length() throws IOException {
        assertThatThrownBy(() -> persistenceManager.readINodeData(
                new INode(1, FileType.FILE, 32, Collections.singletonList(0)),
                new byte[32], 0, -1, 32L
        ))
                .as("negative length exception")
                .isInstanceOf(IllegalArgumentException.class);
        verify(blockStorage, never()).readBlock(anyInt(), any(byte[].class), anyInt(), anyInt(), anyInt());
    }

    @Test
    public void should_fail_to_read_wrong_offset() throws IOException {
        assertThatThrownBy(() -> persistenceManager.readINodeData(
                new INode(1, FileType.FILE, 32, Collections.singletonList(0)),
                new byte[32], -1, 1, 32L
        ))
                .as("negative offset exception")
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> persistenceManager.readINodeData(
                new INode(1, FileType.FILE, 32, Collections.singletonList(0)),
                new byte[32], 32, 1, 32L
        ))
                .as("negative offset exception")
                .isInstanceOf(IllegalArgumentException.class);

        verify(blockStorage, never()).readBlock(anyInt(), any(byte[].class), anyInt(), anyInt(), anyInt());
    }

    @Test
    public void should_write_iNode() throws IOException {
        INode iNode = new INode(1, FileType.FILE, 32L, Collections.singletonList(0));
        persistenceManager.writeINode(iNode);

        verify(blockStorage, times(1)).writeBlock(eq(I_NODES_START_INDEX), any(byte[].class), eq(0), eq(INode.SIZE), eq(INode.SIZE));
    }

    @Test
    public void should_read_iNode() throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(INode.SIZE);
        INode iNode = new INode(1, FileType.FILE, 32L, Collections.singletonList(0));
        iNode.writeTo(byteBuffer);
        byte[] array = byteBuffer.flip().array();

        when(blockStorage.readBlock(anyInt(), any(byte[].class), anyInt(), anyInt(), anyInt()))
                .thenAnswer(inv -> {
                    System.arraycopy(array, 0, inv.<byte[]>getArgument(1), 0, INode.SIZE);
                    return INode.SIZE;
                });

        INode readBack = persistenceManager.readINode(0);
        verify(blockStorage, times(1))
                .readBlock(eq(I_NODES_START_INDEX), any(byte[].class), eq(0), eq(INode.SIZE), eq(0));

        assertThat(readBack)
                .as("read iNode back")
                .isEqualTo(iNode);

        readBack = persistenceManager.readINode(1);
        verify(blockStorage, times(1))
                .readBlock(eq(I_NODES_START_INDEX), any(byte[].class), eq(0), eq(INode.SIZE), eq(INode.SIZE));
    }
}