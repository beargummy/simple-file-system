package net.beargummy.filesystem;

/**
 * Signals that attempt to access to file that is not found in the file system.
 */
public class FileNotFoundException extends RuntimeException {

    FileNotFoundException(String message) {
        super(message);
    }

    FileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
