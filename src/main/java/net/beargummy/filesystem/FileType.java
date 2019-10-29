package net.beargummy.filesystem;

enum FileType {

    FILE(100),
    DIRECTORY(200);

    private final int code;

    FileType(int code) {
        this.code = code;
    }

    static FileType valueOf(int code) {
        for (FileType value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }

    int getCode() {
        return code;
    }
}
