package net.beargummy.filesystem;

/**
 * Signals that attempt to write data to the file system fails due to lack of memory on underlying storage.
 */
public class OutOfMemoryException extends RuntimeException {

    OutOfMemoryException(String message) {
        super(message);
    }

    OutOfMemoryException(String message, Throwable cause) {
        super(message, cause);
    }

}
