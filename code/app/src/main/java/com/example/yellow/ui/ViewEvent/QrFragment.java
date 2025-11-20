package com.example.yellow.ui.ViewEvent;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.yellow.R;
import com.example.yellow.organizers.ViewEventActivity;

public class QrFragment extends Fragment {

    private ImageView qrImage;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔥 Listen for "eventLoaded" after Firestore finishes loading the event
        getParentFragmentManager().setFragmentResultListener(
                "eventLoaded",
                this,
                (key, bundle) -> refreshQr()
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_qr_code_view_events, container, false);
        qrImage = view.findViewById(R.id.qrImage);

        // Try loading immediately (in case event was already loaded before fragment creation)
        refreshQr();

        return view;
    }

    private void refreshQr() {
        ViewEventActivity parent = (ViewEventActivity) getActivity();
        if (parent == null) return;

        if (parent.getEvent() == null) return;

        String qrDataUri = parent.getEvent().getQrImagePng();
        if (qrDataUri != null && !qrDataUri.isEmpty()) {
            displayQr(qrDataUri);
        }
    }

    private void displayQr(String dataUri) {
        try {
            String base64 = dataUri.substring(dataUri.indexOf(",") + 1);
            byte[] bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            qrImage.setImageBitmap(bmp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
