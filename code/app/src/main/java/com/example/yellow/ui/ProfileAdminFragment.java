package com.example.yellow.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.yellow.R;
import com.google.android.material.textfield.TextInputEditText;

public class ProfileAdminFragment extends Fragment {

    private TextInputEditText inputOrgName;
    private TextInputEditText inputEmail;
    private TextInputEditText inputPhone;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
/*
        inputOrgName = v.findViewById(R.id.inputOrgName);
        inputEmail   = v.findViewById(R.id.inputAdminEmail);
        inputPhone   = v.findViewById(R.id.inputAdminPhone);

        MaterialButton btnSave   = v.findViewById(R.id.btnSaveAdmin);
        MaterialButton btnCancel = v.findViewById(R.id.btnCancelAdmin);
        MaterialButton btnDelete = v.findViewById(R.id.btnDeleteAdmin);

        btnSave.setOnClickListener(view -> {
            // TODO: Persist to Firebase / your backend
            Toast.makeText(requireContext(), "Admin settings saved", Toast.LENGTH_SHORT).show();
        });

        btnCancel.setOnClickListener(view -> {
            requireActivity().onBackPressed(); // or popBackStack()
        });

        btnDelete.setOnClickListener(view -> {
            // TODO: Delete admin/organization account (guard with confirmation!)
            Toast.makeText(requireContext(), "Delete org/account not implemented", Toast.LENGTH_SHORT).show();
        });

 */
    }
}