package net.beargummy.filesystem;

import java.io.IOException;
import java.io.File;
import java.io.RandomAccessFile;

/**
 * Filesystem Manager.
 * <p>
 * Primary entry point to create or restore filesystem from given file.
 */
public interface FileSystemManager {

    public static FileSystemManager getInstance() {
        return DefaultFileSystemManager.getInstance();
    }

    /**
     * Create new {@link FileSystem} on given path.
     * Initializes FS structure in the {@code file}.
     * Note: all content of the {@code file} will be erased.
     *
     * @param file       file to use as underlying storage.
     * @param blockSize  block size.
     * @param blockCount number of blocks in file.
     * @return new {@link FileSystem} instance associated with {@code file}.
     * @throws IllegalArgumentException if {@code blockSize} or {@code blockCount} is non-positive
     * @throws NullPointerException     if {@code file} is {@code null}.
     * @throws IOException              if an I/O error occurs.
     */
    public FileSystem create(File file, int blockSize, int blockCount) throws IOException;

    /**
     * Create new {@link FileSystem} on given path.
     * Reads existing FS structure and data from the {@code file}.
     *
     * @param file       file to use as underlying storage.
     * @param blockSize  block size.
     * @param blockCount number of blocks in file.
     * @return new {@link FileSystem} instance associated with {@code file}.
     * @throws IllegalArgumentException if {@code blockSize} or {@code blockCount} is non-positive
     * @throws NullPointerException     if {@code file} is {@code null}.
     * @throws IOException              if an I/O error occurs.
     */
    public FileSystem restore(File file, int blockSize, int blockCount) throws IOException;

    class DefaultFileSystemManager implements FileSystemManager {

        private static class LazyHolder {
            static final FileSystemManager INSTANCE = new DefaultFileSystemManager();
        }

        public static FileSystemManager getInstance() {
            return LazyHolder.INSTANCE;
        }

        @Override
        public FileSystem create(File file, int blockSize, int blockCount) throws IOException {
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            BlockStorage blockStorage = new SingleFileBlockStorage(randomAccessFile, blockSize, blockCount);
            DefaultFileSystem fileSystem = new DefaultFileSystem(blockStorage);
            fileSystem.initFileSystem();
            return fileSystem;
        }

        @Override
        public FileSystem restore(File file, int blockSize, int blockCount) throws IOException {
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            BlockStorage blockStorage = new SingleFileBlockStorage(randomAccessFile, blockSize, blockCount);
            DefaultFileSystem fileSystem = new DefaultFileSystem(blockStorage);
            fileSystem.restoreFileSystem();
            return fileSystem;
        }
    }

}
