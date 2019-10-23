package net.beargummy.filesystem;

import java.io.IOException;

/**
 * Represents a file on FileSystem.
 * <p>
 * Supports methods to access it's content.
 * For a file management {@link FileSystem} should be used instead.
 */
public interface File {

    /**
     * Get file name.
     *
     * @return file name
     */
    // todo: remove?
    public String getName();

    /**
     * Reads up to {@code buffer.length} bytes of data from this file into an array of bytes.
     *
     * @param buffer the buffer into which the data is read.
     * @throws NullPointerException if {@code buffer} is {@code null}.
     * @throws IOException          if an I/O error occurs.
     */
    public void read(byte[] buffer) throws IOException;

    /**
     * Reads up to {@code buffer.length} bytes of data from this file into an array of bytes
     * starting from {@code offset}.
     * {@code offset + destination.length} should not be greater than file size!
     *
     * @param buffer the buffer into which the data is read
     * @param offset an offset in the file starting from which the data is read
     * @throws IllegalArgumentException if {@code offset} is negative or greater than file size
     * @throws NullPointerException     if {@code buffer} is {@code null}.
     * @throws IOException              if an I/O error occurs.
     */
    public void read(byte[] buffer, int offset) throws IOException;

    /**
     * Writes {@code data.length} bytes from the specified byte array to this file.
     *
     * @param data the data.
     * @throws NullPointerException if {@code data} is {@code null}.
     * @throws IOException          if an I/O error occurs.
     */
    public void write(byte[] data) throws IOException;

    /**
     * Writes {@code len} bytes from the specified byte array
     * starting at offset {@code off} to this file.
     *
     * @param data   the data.
     * @param offset the start offset in the file.
     * @throws IllegalArgumentException if {@code offset} is negative or greater than file size
     * @throws NullPointerException     if {@code data} is {@code null}.
     * @throws OutOfMemoryException     if there is no enough space to write content.
     * @throws IOException              if an I/O error occurs.
     */
    void write(byte[] data, int offset) throws IOException;

    /**
     * Get file size in bytes.
     *
     * @return size of the file data space in bytes
     */
    public int getFileSize();

}
