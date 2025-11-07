package com.example.yellow.ui;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.widget.TextView;
import android.widget.Button;
import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.core.app.ApplicationProvider;

import com.example.yellow.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class ProfileUserFragmentTest {

    private FirebaseAuth mockAuth;
    private FirebaseUser mockUser;
    private FirebaseFirestore mockDb;
    private DocumentSnapshot mockDoc;

    @Before
    public void setup() {
        mockAuth = mock(FirebaseAuth.class);
        mockUser = mock(FirebaseUser.class);
        mockDb = mock(FirebaseFirestore.class);
        mockDoc = mock(DocumentSnapshot.class);

        when(mockAuth.getCurrentUser()).thenReturn(mockUser);
        when(mockUser.getUid()).thenReturn("user123");
    }

    // Utility: Launch fragment with mocks injected
    private ProfileUserFragment launchWithMocks() {
        ProfileUserFragment fragment = new ProfileUserFragment();

        fragment.auth = mockAuth;
        fragment.db   = mockDb;

        FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class)
                .create()
                .start()
                .resume()
                .get();

        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(fragment, "test")
                .commitNow();

        return fragment;
    }

    // ✅ Test bindDocToUi()
    @Test
    public void testBindDocToUi_populatesFields() {
        ProfileUserFragment fragment = launchWithMocks();

        View root = fragment.getView();
        assertNotNull(root);

        TextInputEditText fullName = root.findViewById(R.id.inputFullName);
        TextInputEditText email    = root.findViewById(R.id.inputEmail);
        TextInputEditText phone    = root.findViewById(R.id.inputPhone);

        when(mockDoc.exists()).thenReturn(true);
        when(mockDoc.getString("fullName")).thenReturn("John Doe");
        when(mockDoc.getString("email")).thenReturn("john@example.com");
        when(mockDoc.getString("phone")).thenReturn("1234567890");

        fragment.bindDocToUi(mockDoc);

        assertEquals("John Doe", fullName.getText().toString());
        assertEquals("john@example.com", email.getText().toString());
        assertEquals("1234567890", phone.getText().toString());
    }

    // ✅ Test safe()
    @Test
    public void testSafe_nullInputReturnsEmpty() {
        ProfileUserFragment fragment = launchWithMocks();

        View root = fragment.getView();
        TextInputEditText field = root.findViewById(R.id.inputFullName);

        field.setText(null);
        assertEquals("", fragment.safe(field));
    }

    @Test
    public void testSafe_trimsWhitespace() {
        ProfileUserFragment fragment = launchWithMocks();

        View root = fragment.getView();
        TextInputEditText field = root.findViewById(R.id.inputFullName);

        field.setText("   waylon   ");
        assertEquals("waylon", fragment.safe(field));
    }

    // ✅ Test admin toggle (UI)
    @Test
    public void testAdminButton_isHiddenInitially() {
        ProfileUserFragment fragment = launchWithMocks();

        View root = fragment.getView();
        View adminBtn = root.findViewById(R.id.btnAdminDashboard);

        // Should start hidden
        assertEquals(View.GONE, adminBtn.getVisibility());
    }

    // ✅ Test uidOrNull()
    @Test
    public void testUidOrNull() {
        ProfileUserFragment fragment = launchWithMocks();

        assertEquals("user123", fragment.uidOrNull());
    }

    // ✅ SaveProfile — verify Firestore.set() is called
    @Test
    public void testSaveProfile_callsFirestore() {
        ProfileUserFragment fragment = launchWithMocks();

        View root = fragment.getView();

        TextInputEditText name = root.findViewById(R.id.inputFullName);
        TextInputEditText email = root.findViewById(R.id.inputEmail);
        TextInputEditText phone = root.findViewById(R.id.inputPhone);

        name.setText("Waylon");
        email.setText("waylon@example.com");
        phone.setText("123");

        DocumentReference mockDocRef = mock(DocumentReference.class);
        when(mockDb.collection("profiles")).thenReturn(mock(CollectionReference.class));
        when(mockDb.collection("profiles").document("user123")).thenReturn(mockDocRef);

        doReturn(mock(WriteBatch.class));
        mockDocRef.set(anyMap(), eq(SetOptions.merge()));

        fragment.saveProfile();

        verify(mockDb.collection("profiles").document("user123"),
                times(1)).set(anyMap(), eq(SetOptions.merge()));
    }

    // ✅ deleteProfile — verify document.delete()
    @Test
    public void testDeleteProfile_callsFirestoreDelete() {
        ProfileUserFragment fragment = launchWithMocks();

        DocumentReference mockDocRef = mock(DocumentReference.class);

        when(mockDb.collection("profiles")).thenReturn(mock(CollectionReference.class));
        when(mockDb.collection("profiles").document("user123")).thenReturn(mockDocRef);

        fragment.deleteProfile();

        verify(mockDb.collection("profiles").document("user123"), times(1))
                .delete();
    }
}
