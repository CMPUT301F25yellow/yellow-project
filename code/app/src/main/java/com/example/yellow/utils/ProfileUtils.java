package com.example.yellow.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Helper class to enforce profile completeness.
 */
public class ProfileUtils {

    public interface OnCheckComplete {
        void onResult(boolean isComplete);
    }

    /**
     * Checks if the current user has a complete profile (Name + Email).
     * If not, shows an Alert Dialog prompting them to update it.
     *
     * @param context    The Activity/Fragment context.
     * @param onComplete Callback with true if complete, false if incomplete (or
     *                   error).
     */
    public static void checkProfile(Context context, OnCheckComplete onComplete, Runnable onGoToProfile) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "Not logged in", Toast.LENGTH_SHORT).show();
            onComplete.onResult(false);
            return;
        }

        FirebaseFirestore.getInstance().collection("profiles")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (isProfileComplete(doc)) {
                        onComplete.onResult(true);
                    } else {
                        showIncompleteDialog(context, onGoToProfile);
                        onComplete.onResult(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to check profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    onComplete.onResult(false);
                });
    }

    private static boolean isProfileComplete(DocumentSnapshot doc) {
        if (doc == null || !doc.exists())
            return false;
        String name = doc.getString("fullName");
        String email = doc.getString("email");
        return !TextUtils.isEmpty(name) && !TextUtils.isEmpty(email);
    }

    private static void showIncompleteDialog(Context context, Runnable onGoToProfile) {
        new AlertDialog.Builder(context)
                .setTitle("Profile Incomplete")
                .setMessage("You must provide your Full Name and Email to perform this action.")
                .setPositiveButton("Go to Profile", (dialog, which) -> {
                    if (onGoToProfile != null)
                        onGoToProfile.run();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
