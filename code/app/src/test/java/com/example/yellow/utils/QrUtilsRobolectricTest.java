package com.example.yellow.utils;

import static org.junit.Assert.*;

import android.graphics.Bitmap;

import com.google.zxing.WriterException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class QrUtilsRobolectricTest {

    @Test
    public void makeQr_returnsBitmapWithCorrectSize() throws WriterException {
        // Arrange
        String content = "yellow://event/TEST_EVENT_ID";
        int size = 512;

        // Act
        Bitmap qr = QrUtils.makeQr(content, size);

        // Assert
        assertNotNull("makeQr should not return null", qr);
        assertEquals("Width should match requested size", size, qr.getWidth());
        assertEquals("Height should match requested size", size, qr.getHeight());
    }
}
