package net.beargummy.filesystem;

import java.io.IOException;

public class OutOfMemoryException extends IOException {

    public OutOfMemoryException() {
    }

    public OutOfMemoryException(String message) {
        super(message);
    }
}
