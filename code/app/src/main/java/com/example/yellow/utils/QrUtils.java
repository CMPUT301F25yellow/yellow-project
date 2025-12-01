package com.example.yellow.utils;

import android.graphics.Bitmap;
import android.net.Uri;

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
 * Parses the event ID from a QR code.
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

    /**
     * Parses a raw URI string from a QR code and returns the event ID if it's a valid app link.
     *
     * @param rawValue The scanned string from the QR code.
     * @return The event ID, or null if the URI is invalid or doesn't match the expected format.
     */
    public static String getEventIdFromUri(String rawValue) {
        if (rawValue == null || rawValue.isEmpty()) {
            return null;
        }

        try {
            Uri uri = Uri.parse(rawValue);
            // Check for scheme "yellow" and host "eventdetails", yellow://eventdetails
            if ("yellow".equals(uri.getScheme()) && "eventdetails".equals(uri.getHost())) {
                String eventId = uri.getLastPathSegment();
                if (eventId != null && !eventId.isEmpty()) {
                    return eventId;
                }
            }
        } catch (Exception e) {
            // Invalid URI format, etc.
            return null;
        }

        return null; // Not a valid app link
    }
}
