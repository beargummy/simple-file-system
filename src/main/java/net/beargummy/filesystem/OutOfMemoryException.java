package net.beargummy.filesystem;

public class OutOfMemoryException extends RuntimeException {

    OutOfMemoryException() {
    }

    OutOfMemoryException(String message) {
        super(message);
    }

    OutOfMemoryException(String message, Throwable cause) {
        super(message, cause);
    }

    OutOfMemoryException(Throwable cause) {
        super(cause);
    }
}
