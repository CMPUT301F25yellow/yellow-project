package com.example.yellow.utils;

import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.HashMap;
import java.util.Map;

/**
 * Makes QR code bitmaps from text (e.g., "yellow://event/<docId>").
 */
public final class QrUtils {
    private QrUtils() {}

    /**
     * Creates a square QR code bitmap.
     *
     * @param content Text to encode (not null/empty)
     * @param sizePx  Width/height in pixels (e.g., 512–1024)
     * @return QR code bitmap
     * @throws WriterException If encoding fails
     */
    public static Bitmap makeQr(String content, int sizePx) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1); // small border

        BitMatrix matrix = new MultiFormatWriter()
                .encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);

        return new BarcodeEncoder().createBitmap(matrix);
    }
}
