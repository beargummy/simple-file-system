package net.beargummy.filesystem;

public class NoSuchFileException extends RuntimeException {

    NoSuchFileException() {
    }

    NoSuchFileException(String message) {
        super(message);
    }

    NoSuchFileException(String message, Throwable cause) {
        super(message, cause);
    }

    NoSuchFileException(Throwable cause) {
        super(cause);
    }
}
