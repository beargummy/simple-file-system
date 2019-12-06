package net.beargummy.filesystem;

import java.io.IOException;

public interface BlockStorage {

    /**
     * Reads up to {@code buffer.length} bytes of block buffer into an array of bytes.
     *
     * @param blockNumber index of block to be returned
     * @param buffer      the buffer into which the buffer is read.
     * @return the total number of bytes read into the buffer, or -1 if there is no more data
     * @throws NullPointerException if {@code buffer} is {@code null}.
     * @throws IOException          if an I/O error occurs.
     */
    int readBlock(int blockNumber, byte[] buffer) throws IOException;

    /**
     * Reads up to {@code buffer.length} bytes of block buffer into an array of bytes.
     *
     * @param blockNumber index of block to be returned
     * @param buffer      the buffer into which the buffer is read.
     * @param offset      offset in buffer.
     * @param length      amount of bytes to read to buffer.
     * @param position    start position in the block.
     * @return the total number of bytes read into the buffer, or -1 if there is no more data
     * @throws NullPointerException if {@code buffer} is {@code null}.
     * @throws IOException          if an I/O error occurs.
     */
    int readBlock(int blockNumber, byte[] buffer, int offset, int length, int position) throws IOException;

    /**
     * Writes raw buffer to the block.
     *
     * @param blockNumber index of block to write to.
     * @param buffer      buffer with data.
     * @throws IllegalArgumentException if {@code buffer.length} is greater than block size.
     * @throws NullPointerException     if {@code buffer} is {@code null}.
     * @throws IOException              if an I/O error occurs.
     */
    void writeBlock(int blockNumber, byte[] buffer) throws IOException;

    /**
     * Writes raw buffer to the block.
     *
     * @param blockNumber index of block to write to.
     * @param buffer      buffer with data.
     * @param offset      start offset in buffer.
     * @param length      length of bytes to write to block. should smaller or equal to {@code buffer.length} minus {@code offset}.
     * @param position    start position in the block.
     * @throws IllegalArgumentException if {@code position} plus {@code length} is greater than block size.
     * @throws IllegalArgumentException if {@code length} is greater than {@code buffer.length} minus {@code offset}.
     * @throws IllegalArgumentException if {@code offset} plus {@code length} is greater than block size
     * @throws NullPointerException     if {@code buffer} is {@code null}.
     * @throws IOException              if an I/O error occurs.
     */
    void writeBlock(int blockNumber, byte[] buffer, int offset, int length, int position) throws IOException;

    /**
     * Get block size in bytes.
     *
     * @return block size in bytes.
     */
    int getBlockSize();

    /**
     * Get max amount of blocks in storage.
     *
     * @return max amount of blocks.
     */
    int getBlocksCount();

}
