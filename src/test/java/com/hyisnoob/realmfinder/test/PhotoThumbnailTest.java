package com.hyisnoob.realmfinder.test;

import com.hyisnoob.realmfinder.core.snapshot.PhotoThumbnail;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhotoThumbnailTest {
    @Test
    void acceptsOnlyBoundedPngHeader() {
        byte[] valid = pngHeader(128, 128);
        assertTrue(PhotoThumbnail.isValid(valid));
        assertFalse(PhotoThumbnail.isValid(pngHeader(4096, 4096)));
        valid[0] = 0;
        assertFalse(PhotoThumbnail.isValid(valid));
    }

    private static byte[] pngHeader(int width, int height) {
        byte[] bytes = new byte[33];
        byte[] signature = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
        System.arraycopy(signature, 0, bytes, 0, signature.length);
        bytes[11] = 13;
        bytes[12] = 73;
        bytes[13] = 72;
        bytes[14] = 68;
        bytes[15] = 82;
        bytes[16] = (byte) (width >>> 24);
        bytes[17] = (byte) (width >>> 16);
        bytes[18] = (byte) (width >>> 8);
        bytes[19] = (byte) width;
        bytes[20] = (byte) (height >>> 24);
        bytes[21] = (byte) (height >>> 16);
        bytes[22] = (byte) (height >>> 8);
        bytes[23] = (byte) height;
        return bytes;
    }
}
