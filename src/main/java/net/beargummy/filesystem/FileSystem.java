package net.beargummy.filesystem;

import java.io.IOException;

/**
 * File system interface.
 * Using this to operate on files.
 */
public interface FileSystem {

    /**
     * Create new empty {@link File file} with given name.
     *
     * @param name file name
     * @return {@link File} instance
     * @throws NullPointerException     if file name is {@code null}.
     * @throws IllegalArgumentException if file name is empty string.
     * @throws FileAlreadyExists        if file already exists.
     * @throws IOException              if an I/O error occurs.
     */
    public File createFile(String name) throws IOException;

    /**
     * Delete {@link File file} with given file name.
     *
     * @param name file name
     * @throws NullPointerException     if file name is {@code null}.
     * @throws IllegalArgumentException if file name is empty string.
     * @throws NoSuchFileException      if the file does not exist.
     * @throws IOException              if an I/O error occurs.
     */
    public void deleteFile(String name) throws IOException;

}
