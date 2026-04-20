package com.example.cs360_charlton_molloy_keir;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.cs360_charlton_molloy_keir.databinding.FragmentFirstBinding;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;

    // Database helper used for login and account creation
    private AppDatabaseHelper db;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = new AppDatabaseHelper(requireContext());

        // Log in: validate username/password against the Users table
        binding.buttonFirst.setOnClickListener(v -> {
            String username = binding.editUsername.getText().toString().trim();
            String password = binding.editPassword.getText().toString();

            // Check required fields
            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                ToastUtil.show(requireContext(), R.string.toast_enter_username_password);
                return;
            }

            long userId = db.validateLogin(username, password);

            // If login fails, clear password and show message
            if (userId == -1) {
                ToastUtil.show(requireContext(), R.string.toast_login_failed);
                binding.editPassword.setText("");
                return;
            }

            // Clear password before navigating away
            binding.editPassword.setText("");

            // Save session and open the history screen
            Session.storeLoggedInUser(requireContext(), userId);
            NavHostFragment.findNavController(FirstFragment.this)
                    .navigate(R.id.action_FirstFragment_to_SecondFragment);
        });

        // Create account: insert a new row in the Users table
        binding.buttonCreateAccount.setOnClickListener(v -> {
            String username = binding.editUsername.getText().toString().trim();
            String password = binding.editPassword.getText().toString();

            // Check required fields
            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                ToastUtil.show(requireContext(), R.string.toast_enter_username_password);
                return;
            }

            // Prevent duplicate usernames
            if (db.usernameExists(username)) {
                ToastUtil.show(requireContext(), R.string.toast_username_taken);
                binding.editPassword.setText("");
                return;
            }

            long newUserId = db.createUser(username, password);

            // If insert fails, show message
            if (newUserId == -1) {
                ToastUtil.show(requireContext(), R.string.toast_create_account_failed);
                binding.editPassword.setText("");
                return;
            }

            // Clear password before navigating away
            binding.editPassword.setText("");

            // Save session and open the history screen
            Session.storeLoggedInUser(requireContext(), newUserId);
            NavHostFragment.findNavController(FirstFragment.this)
                    .navigate(R.id.action_FirstFragment_to_SecondFragment);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Close the database helper when the view is destroyed
        if (db != null) {
            db.close();
            db = null;
        }

        binding = null;
    }
}