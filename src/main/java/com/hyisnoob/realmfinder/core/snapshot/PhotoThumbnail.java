package com.hyisnoob.realmfinder.core.snapshot;

public final class PhotoThumbnail {
    public static final int SIZE = 128;
    public static final int MAX_BYTES = 128 * 1024;
    private static final byte[] PNG_SIGNATURE = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};

    private PhotoThumbnail() {}

    public static boolean isValid(byte[] bytes) {
        if (bytes == null || bytes.length < 33 || bytes.length > MAX_BYTES) return false;
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (bytes[i] != PNG_SIGNATURE[i]) return false;
        }
        return readInt(bytes, 8) == 13
                && bytes[12] == 'I' && bytes[13] == 'H' && bytes[14] == 'D' && bytes[15] == 'R'
                && readInt(bytes, 16) == SIZE && readInt(bytes, 20) == SIZE;
    }

    private static int readInt(byte[] bytes, int index) {
        return ((bytes[index] & 255) << 24) | ((bytes[index + 1] & 255) << 16)
                | ((bytes[index + 2] & 255) << 8) | (bytes[index + 3] & 255);
    }
}
