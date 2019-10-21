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
     */
    public File createFile(String name) throws IOException;

    /**
     * Delete {@link File file} with given file name.
     *
     * @param name file name
     * @return {@code true} if delete was successful, {@code false} otherwise
     */
    public boolean deleteFile(String name);

}
