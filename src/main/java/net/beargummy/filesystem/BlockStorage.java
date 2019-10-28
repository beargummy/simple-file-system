package net.beargummy.filesystem;

import java.io.IOException;

public interface BlockStorage {

    /**
     * Reads up to {@code buffer.length} bytes of block data into an array of bytes.
     *
     * @param index  of block to be returned
     * @param buffer the buffer into which the data is read.
     * @throws NullPointerException if {@code buffer} is {@code null}.
     * @throws IOException          if an I/O error occurs.
     */
    void readBlock(byte[] buffer, int index) throws IOException;

    /**
     * Reads up to {@code buffer.length} bytes of block data into an array of bytes.
     *
     * @param index  of block to be returned
     * @param buffer the buffer into which the data is read.
     * @param offset start offset in block.
     * @throws NullPointerException if {@code buffer} is {@code null}.
     * @throws IOException          if an I/O error occurs.
     */
    void readBlock(byte[] buffer, int index, int offset) throws IOException;

    /**
     * Writes raw data to the block.
     *
     * @param data  block data.
     * @param index index of block to write to.
     * @throws IllegalArgumentException if {@code data.length} is greater than block size.
     * @throws NullPointerException     if {@code buffer} is {@code null}.
     * @throws IOException              if an I/O error occurs.
     */
    void writeBlock(byte[] data, int index) throws IOException;

    /**
     * Writes raw data to the block.
     *
     * @param data   block data.
     * @param index  index of block to write to.
     * @param offset start offset in block.
     * @throws IllegalArgumentException if {@code offset} plus {@code data.length} is greater than block size.
     * @throws NullPointerException     if {@code buffer} is {@code null}.
     * @throws IOException              if an I/O error occurs.
     */
    void writeBlock(byte[] data, int index, int offset) throws IOException;

    /**
     * Writes raw data to the block.
     *
     * @param data   block data.
     * @param index  index of block to write to.
     * @param offset start offset in block.
     * @param length length of bytes to write to block. should smaller or equal to {@code data.length}.
     * @throws IllegalArgumentException if {@code offset} plus {@code data.length} is greater than block size.
     * @throws IllegalArgumentException if {@code length} is greater than {@code data.length}.
     * @throws IllegalArgumentException if {@code offset} plus maximum of {@code data.length} and {@code length} is greater than block size
     * @throws NullPointerException     if {@code buffer} is {@code null}.
     * @throws IOException              if an I/O error occurs.
     */
    void writeBlock(byte[] data, int index, int offset, int length) throws IOException;

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
