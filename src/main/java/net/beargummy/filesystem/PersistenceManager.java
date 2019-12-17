package net.beargummy.filesystem;

import java.io.IOException;
import java.nio.ByteBuffer;

class PersistenceManager {

    private final BlockStorage blockStorage;
    private final DefaultFileSystem fileSystem;

    private final long iNodesStartIndex;
    private final long dataNodesStartIndex;

    private final int blockSize;

    PersistenceManager(BlockStorage blockStorage, DefaultFileSystem fileSystem,
                       long iNodesStartIndex, long dataNodesStartIndex) {
        this.blockStorage = blockStorage;
        this.fileSystem = fileSystem;
        this.iNodesStartIndex = iNodesStartIndex;
        this.dataNodesStartIndex = dataNodesStartIndex;

        this.blockSize = blockStorage.getBlockSize();
    }

    INode readINode(long iNodeIndex) throws IOException {
        int iNodeLength = INode.SIZE;
        int iNodesPerBlock = blockSize / iNodeLength;
        long iNodeBlock = (iNodeIndex / iNodesPerBlock) + iNodesStartIndex;
        long iNodePositionInBlock = (iNodeIndex % iNodesPerBlock) * iNodeLength;

        ByteBuffer byteBuffer = ByteBuffer.allocate(iNodeLength);

        byte[] array = byteBuffer.array();
        blockStorage.readBlock(iNodeBlock, array, 0, iNodeLength, iNodePositionInBlock);
        return new INode(fileSystem, byteBuffer);
    }

    void writeINode(INode iNode) throws IOException {
        int iNodeLength = INode.SIZE;
        long iNodeIndex = iNode.getINodeNumber();
        int iNodesPerBlock = blockSize / iNodeLength;
        long iNodeBlock = (iNodeIndex / iNodesPerBlock) + iNodesStartIndex;
        long iNodePositionInBlock = (iNodeIndex % iNodesPerBlock) * iNodeLength;
        ByteBuffer byteBuffer = ByteBuffer.allocate(iNodeLength);
        iNode.writeTo(byteBuffer);
        byte[] array = byteBuffer.array();
        blockStorage.writeBlock(iNodeBlock, array, 0, iNodeLength, iNodePositionInBlock);
    }

    int readINodeData(INode iNode, byte[] buffer, int offset, int length, long position) throws IOException {
        verifyArguments(buffer, offset, length, position);

        int bytesRead = 0;
        long dataBlocksCount = iNode.getDataBlocksCount();
        if (position / blockSize > dataBlocksCount) {
            return 0;
        }

        long iNodeSize = iNode.getSize();
        int bytesToRead = (int) Math.min(iNodeSize - position, length);

        if (bytesToRead == 0) {
            return 0;
        }

        long blocksNeeded = position + bytesToRead > blockSize
                ? ((position + bytesToRead - 1) / blockSize) + 1
                : 1;
        blocksNeeded = Math.min(blocksNeeded, dataBlocksCount);

        ByteBuffer byteBuffer = ByteBuffer.allocate(blockSize);

        long firstBlockOffsetToRead = (int) (position / blockSize);
        long lastBlockToRead = blocksNeeded - 1;
        for (long block = firstBlockOffsetToRead; block <= lastBlockToRead; block++) {
            long currentPosition = block == firstBlockOffsetToRead ? (int) (position % blockSize) : 0;
            int currentLength = bytesToRead >= blockSize
                    ? blockSize
                    : (int) Math.min(bytesToRead, blockSize - currentPosition);
            long currentBlockNumber = iNode.getBlockByIndex(block);

            byteBuffer.clear();
            int fetchedFromBS = blockStorage.readBlock(dataNodesStartIndex + currentBlockNumber, byteBuffer.array(), 0, currentLength, currentPosition);
            if (fetchedFromBS == -1) {
                return bytesRead;
            }
            byteBuffer.get(buffer, bytesRead + offset, fetchedFromBS);
            bytesRead += fetchedFromBS;
            bytesToRead -= fetchedFromBS;
            if (fetchedFromBS != currentLength) {
                return bytesRead;
            }
        }
        return bytesRead;
    }

    void writeDataBlock(byte[] data, int offset, int length, long position, long block) throws IOException {
        verifyArguments(data, offset, length, position);
        if (length + position > blockSize) {
            throw new IllegalArgumentException("Length is greater than block size");
        }

        blockStorage.writeBlock(dataNodesStartIndex + block, data, offset, length, position);
    }

    int readDataBlock(byte[] buffer, int offset, int length, long position, long block) throws IOException {
        verifyArguments(buffer, offset, length, position);
        if (length + position > blockSize) {
            throw new IllegalArgumentException("Length is greater than block size");
        }

        return blockStorage.readBlock(dataNodesStartIndex + block, buffer, offset, length, position);
    }

    int writeINodeData(INode iNode, byte[] data, int offset, int length, long position) throws IOException {
        verifyArguments(data, offset, length, position);

        long iNodeSize = iNode.getSize();
        long newSize = Math.max(position + length, iNodeSize);

        long blocksNeeded = (long) Math.ceil(((double) newSize) / blockSize);
        long firstBlockOffsetToWrite = position / blockSize;
        long lastBlockToWrite = blocksNeeded - firstBlockOffsetToWrite - 1;
        lastBlockToWrite = lastBlockToWrite == 0 ? firstBlockOffsetToWrite : lastBlockToWrite;

        int bytesWritten = 0;
        ByteBuffer byteBuffer = ByteBuffer.allocate(blockSize);
        for (long block = firstBlockOffsetToWrite; block <= lastBlockToWrite; block++) {
            int currentBlockPosition = block == firstBlockOffsetToWrite ? (int) (position % blockSize) : 0;

            int currentLength;
            if (block == firstBlockOffsetToWrite) {
                currentLength = Math.min(length, blockSize - currentBlockPosition);
            } else if (block == lastBlockToWrite) {
                currentLength = Math.min(length - bytesWritten, blockSize - currentBlockPosition);
            } else {
                currentLength = Math.min(blockSize, length) - currentBlockPosition % Math.min(blockSize, length);
            }
            long currentBlockNumber = iNode.getOrCreateBlockByIndex(block);
            if (currentBlockNumber == -1) {
                break;
            }

            byteBuffer.clear();
            byteBuffer.put(data, bytesWritten + offset, currentLength);
            byteBuffer.flip();
            blockStorage.writeBlock(dataNodesStartIndex + currentBlockNumber, byteBuffer.array(), 0, currentLength, currentBlockPosition);
            bytesWritten += currentLength;
        }

        iNode.setSize(newSize);
        return bytesWritten;
    }

    BitMap readBitMap(long blockNumber) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(blockSize);

        byteBuffer.clear();
        blockStorage.readBlock(blockNumber, byteBuffer.array());
        return new BitMap(byteBuffer);
    }

    void writeBitMap(BitMap bitMap, long blockNumber) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(blockSize);
        bitMap.writeTo(byteBuffer);
        blockStorage.writeBlock(blockNumber, byteBuffer.array());
    }

    private void verifyArguments(byte[] buffer, int offset, int length, long position) {
        if (null == buffer) {
            throw new NullPointerException("Buffer is null");
        }
        if (length < 0) {
            throw new IllegalArgumentException("Length is negative");
        }
        if (length > buffer.length) {
            throw new IllegalArgumentException("Length is greater than buffer's length");
        }
        if (offset >= buffer.length) {
            throw new IllegalArgumentException("Offset is greater than buffer's length");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }
        if (position < 0) {
            throw new IllegalArgumentException("Position is negative");
        }
    }
}
