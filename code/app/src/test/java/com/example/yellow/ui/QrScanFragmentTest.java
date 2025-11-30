package com.example.yellow.ui;

import android.os.Looper;
import androidx.fragment.app.testing.FragmentScenario;

import com.example.yellow.R;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.google.mlkit.vision.common.InputImage;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Test the isScanning flag in QrScanFragment.
 * Qr parsing is tested in QrUtils.
 *
 * @author Marcus Lau mdlau
 */
@RunWith(RobolectricTestRunner.class)
public class QrScanFragmentTest {

    @Test
    public void handleScannedQrCode_withInvalidUri_resumesScanningAfterDelay() {

        // 1. Arrange: Mock the BarcodeScanner and the static BarcodeScanning.getClient() method
        BarcodeScanner mockScanner = mock(BarcodeScanner.class);
        // This tells Mockito to intercept all calls to BarcodeScanning.getClient()
        try (MockedStatic<BarcodeScanning> mocked = mockStatic(BarcodeScanning.class)) {
            // When getClient() is called anywhere, return our fake scanner instead of the real one.
            when(BarcodeScanning.getClient()).thenReturn(mockScanner);
            // We also need to mock the Task it returns to avoid crashes.
            when(mockScanner.process(any(InputImage.class))).thenReturn(Tasks.forResult(Collections.emptyList()));

            FragmentScenario<QrScanFragment> scenario = FragmentScenario.launchInContainer(QrScanFragment.class, null, R.style.Theme_Yellow);
            String invalidRawValue = "this-is-not-a-valid-uri";

            scenario.onFragment(fragment -> {
                // 2. Act
                fragment.isScanning = false;
                fragment.handleScannedQrCode(invalidRawValue);

                // 3. Assert (Immediate)
                assertFalse("isScanning should be false immediately after processing an invalid code.", fragment.isScanning);

                // 4. Act (Advance time)
                ShadowLooper.idleMainLooper(2, TimeUnit.SECONDS);

                // 5. Assert (After Delay)
                assertTrue("isScanning should be true after the 2-second delay.", fragment.isScanning);
            });
        }
    }
}
