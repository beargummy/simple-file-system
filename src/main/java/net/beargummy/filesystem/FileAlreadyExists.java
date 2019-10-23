package net.beargummy.filesystem;

public class FileAlreadyExists extends RuntimeException {

    FileAlreadyExists() {
    }

    FileAlreadyExists(String message) {
        super(message);
    }

    FileAlreadyExists(String message, Throwable cause) {
        super(message, cause);
    }

    FileAlreadyExists(Throwable cause) {
        super(cause);
    }

}
