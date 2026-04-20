package com.example.cs360_charlton_molloy_keir;

import android.Manifest;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.cs360_charlton_molloy_keir.databinding.FragmentSmsBinding;

public class SmsFragment extends Fragment {

    private FragmentSmsBinding binding;

    // Keep a reference so the listener can be temporarily detached during programmatic switch changes
    private CompoundButton.OnCheckedChangeListener smsToggleListener;

    // Current logged-in user id
    private long userId;

    // Launcher used to request the SEND_SMS permission
    private final ActivityResultLauncher<String> requestSmsPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!isAdded() || binding == null) return;

                updatePermissionStatus(granted);

                // If permission was denied, turn alerts off and continue app
                if (!granted) {
                    ToastUtil.show(requireContext(), R.string.sms_permission_denied_toast);
                    Session.setSmsEnabled(requireContext(), userId, false);
                    setAlertsSwitchChecked(false);
                    updateAlertsStatus(false, false);
                    return;
                }

                // Permission granted, now enable SMS only if phone is valid
                enableSmsIfPhoneValid();
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSmsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userId = Session.getLoggedInUserId(requireContext());

        // If no user is logged in, go back to the previous screen
        if (userId == -1) {
            ToastUtil.show(requireContext(), R.string.toast_login_first);
            NavHostFragment.findNavController(SmsFragment.this).popBackStack();
            return;
        }

        // Load the saved phone number for the current user
        binding.editSmsPhone.setText(Session.getSmsPhoneNumber(requireContext(), userId));

        // Save button validates and stores the destination number
        binding.buttonSavePhone.setOnClickListener(v -> savePhoneNumber());

        // Listener for the SMS toggle
        smsToggleListener = (buttonView, isChecked) -> handleSmsToggle(isChecked);
        binding.switchSmsAlerts.setOnCheckedChangeListener(smsToggleListener);

        // Populate the screen based on current permission and saved settings
        refreshStatus();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Refresh when returning from Android Settings
        refreshStatus();
    }

    // Function to refresh the permission status and toggle status text
    private void refreshStatus() {
        if (binding == null || !isAdded()) return;

        boolean hasPermission = SmsUtil.hasSendSmsPermission(requireContext());
        updatePermissionStatus(hasPermission);

        boolean enabledPref = Session.isSmsEnabled(requireContext(), userId);

        // Only show the switch as ON if both the preference is ON and permission is granted
        setAlertsSwitchChecked(enabledPref && hasPermission);

        updateAlertsStatus(enabledPref, hasPermission);
    }

    // Function to update the "alerts on/off" status label
    private void updateAlertsStatus(boolean enabledForAccount, boolean hasPermission) {
        if (binding == null) return;

        boolean effectiveOn = enabledForAccount && hasPermission;
        binding.textSmsAlertsStatusValue.setText(
                effectiveOn ? R.string.sms_alerts_status_on : R.string.sms_alerts_status_off
        );
    }

    // Function to update the "permission granted/not granted" status label
    private void updatePermissionStatus(boolean granted) {
        if (binding == null) return;

        binding.textSmsPermissionStatusValue.setText(
                granted ? R.string.sms_permission_status_granted : R.string.sms_permission_status_not_granted
        );
    }

    // Function to set the switch state without triggering the listener again
    private void setAlertsSwitchChecked(boolean checked) {
        if (binding == null) return;

        binding.switchSmsAlerts.setOnCheckedChangeListener(null);
        binding.switchSmsAlerts.setChecked(checked);
        binding.switchSmsAlerts.setOnCheckedChangeListener(smsToggleListener);
    }

    // Function to validate and save the phone number
    private void savePhoneNumber() {
        String rawPhone = binding.editSmsPhone.getText().toString();
        String sanitized = SmsUtil.sanitizePhoneNumber(rawPhone);

        // If no number was entered, show an error
        if (TextUtils.isEmpty(sanitized)) {
            binding.editSmsPhone.setError(getString(R.string.sms_phone_missing));
            ToastUtil.show(requireContext(), R.string.sms_phone_missing);
            return;
        }

        // If the number is not valid, show an error
        if (!SmsUtil.isValidDestinationPhoneNumber(sanitized)) {
            binding.editSmsPhone.setError(getString(R.string.sms_phone_invalid));
            ToastUtil.show(requireContext(), R.string.sms_phone_invalid);
            return;
        }

        // Save the number for the current user
        binding.editSmsPhone.setError(null);
        Session.setSmsPhoneNumber(requireContext(), userId, sanitized);
        binding.editSmsPhone.setText(sanitized);

        ToastUtil.show(requireContext(), R.string.sms_phone_saved_for_account);
    }

    // Function that runs when the user toggles SMS alerts
    private void handleSmsToggle(boolean isChecked) {
        if (binding == null || !isAdded()) return;

        // If user turned it off, save the preference and stop
        if (!isChecked) {
            Session.setSmsEnabled(requireContext(), userId, false);
            updateAlertsStatus(false, SmsUtil.hasSendSmsPermission(requireContext()));
            return;
        }

        // If permission is not granted, ask for it
        if (!SmsUtil.hasSendSmsPermission(requireContext())) {
            showPermissionDialog();
            return;
        }

        // Permission is granted, so try enabling SMS
        enableSmsIfPhoneValid();
    }

    // Function to show a simple permission dialog before the system permission prompt
    private void showPermissionDialog() {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.sms_permission_dialog_title)
                .setMessage(getString(R.string.sms_permission_required))
                .setPositiveButton(R.string.action_continue, (d, which) ->
                        requestSmsPermissionLauncher.launch(Manifest.permission.SEND_SMS))
                .setNegativeButton(R.string.action_not_now, (d, which) -> {
                    // User chose not to continue, so turn alerts off
                    Session.setSmsEnabled(requireContext(), userId, false);
                    setAlertsSwitchChecked(false);
                    updatePermissionStatus(false);
                    updateAlertsStatus(false, false);
                })
                .setOnCancelListener(d -> {
                    // If user cancels/back, turn alerts off
                    Session.setSmsEnabled(requireContext(), userId, false);
                    setAlertsSwitchChecked(false);
                    updatePermissionStatus(false);
                    updateAlertsStatus(false, false);
                })
                .create();

        // Prevent "tap outside to dismiss" leaving the switch in a weird state
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    // Function to enable SMS only if a valid phone number exists
    private void enableSmsIfPhoneValid() {
        boolean hasPermission = SmsUtil.hasSendSmsPermission(requireContext());
        updatePermissionStatus(hasPermission);

        // Get saved phone first
        String phone = Session.getSmsPhoneNumber(requireContext(), userId);
        String sanitizedPhone = SmsUtil.sanitizePhoneNumber(phone);

        // If nothing saved, try what the user typed in the field
        if (TextUtils.isEmpty(sanitizedPhone)) {
            String typed = binding.editSmsPhone.getText().toString();
            sanitizedPhone = SmsUtil.sanitizePhoneNumber(typed);
        }

        // If still missing, turn alerts off
        if (TextUtils.isEmpty(sanitizedPhone)) {
            ToastUtil.show(requireContext(), R.string.sms_phone_missing);
            Session.setSmsEnabled(requireContext(), userId, false);
            setAlertsSwitchChecked(false);
            updateAlertsStatus(false, hasPermission);
            return;
        }

        // If invalid, turn alerts off and show error
        if (!SmsUtil.isValidDestinationPhoneNumber(sanitizedPhone)) {
            binding.editSmsPhone.setError(getString(R.string.sms_phone_invalid));
            ToastUtil.show(requireContext(), R.string.sms_phone_invalid);
            Session.setSmsEnabled(requireContext(), userId, false);
            setAlertsSwitchChecked(false);
            updateAlertsStatus(false, hasPermission);
            return;
        }

        // Save normalized phone and enable alerts
        Session.setSmsPhoneNumber(requireContext(), userId, sanitizedPhone);
        binding.editSmsPhone.setText(sanitizedPhone);

        Session.setSmsEnabled(requireContext(), userId, true);
        setAlertsSwitchChecked(true);
        updateAlertsStatus(true, hasPermission);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}