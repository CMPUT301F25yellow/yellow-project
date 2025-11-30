package com.example.yellow.organizers;

import static org.junit.Assert.*;

import android.graphics.Bitmap;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.yellow.utils.QrUtils;
import com.google.zxing.WriterException;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class QrUtilsTest {

    @Test
    public void makeQr_returnsBitmapOfCorrectSize() throws WriterException {
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
