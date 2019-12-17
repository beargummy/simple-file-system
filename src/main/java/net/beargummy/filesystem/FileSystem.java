package net.beargummy.filesystem;

import java.io.IOException;

/**
 * File system interface.
 */
public interface FileSystem extends AutoCloseable {

    /**
     * Create new empty {@link File file} with given name.
     * <p>
     * The {@code name} can be either absolute path name with directories separated by FS path separator,
     * or simple file name if root directory should be used.
     *
     * @param name file name
     * @return {@link File} instance
     * @throws NullPointerException     if file name is {@code null}.
     * @throws IllegalArgumentException if file name is empty string.
     * @throws FileAlreadyExists        if file already exists.
     * @throws IllegalStateException    if FileSystem is closed
     * @throws IOException              if an I/O error occurs.
     */
    File createFile(String name) throws IOException;

    /**
     * Open {@link File file} with given file name.
     * <p>
     * The {@code name} can be either absolute path name with directories separated by FS path separator,
     * or simple file name if root directory should be used.
     *
     * @param name file name
     * @throws NullPointerException     if file name is {@code null}.
     * @throws IllegalArgumentException if file name is empty string.
     * @throws FileNotFoundException    if the file does not exist.
     * @throws IllegalStateException    if FileSystem is closed
     * @throws IOException              if an I/O error occurs.
     */
    File openFile(String name) throws IOException;

    /**
     * Delete {@link File file} with given file name.
     * <p>
     * The {@code name} can be either absolute path name with directories separated by FS path separator,
     * or simple file name if root directory should be used.
     *
     * @param name file name
     * @throws NullPointerException     if file name is {@code null}.
     * @throws IllegalArgumentException if file name is empty string.
     * @throws FileNotFoundException    if the file does not exist.
     * @throws IllegalStateException    if FileSystem is closed
     * @throws IOException              if an I/O error occurs.
     */
    void deleteFile(String name) throws IOException;

    /**
     * Closes {@code FileSystem} and underlying {@link BlockStorage}.
     *
     * @throws IOException if an I/O error occurs.
     * @throws Exception   if other error occurs.
     */
    @Override
    void close() throws Exception;
}
