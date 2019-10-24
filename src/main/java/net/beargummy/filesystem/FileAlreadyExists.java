package net.beargummy.filesystem;

/**
 * Signals an attempt to create the file that already exists.
 */
public class FileAlreadyExists extends RuntimeException {

    FileAlreadyExists(String message) {
        super(message);
    }

    FileAlreadyExists(String message, Throwable cause) {
        super(message, cause);
    }

}
