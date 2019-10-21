package net.beargummy.filesystem;

enum FileType {

    UNKNOWN(0),
    FILE(100),
    DIRECTORY(200);

    private final int code;

    FileType(int code) {
        this.code = code;
    }

    public static FileType valueOf(int code) {
        for (FileType value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }

    public int getCode() {
        return code;
    }
}
