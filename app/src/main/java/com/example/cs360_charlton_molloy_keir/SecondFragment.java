package com.example.cs360_charlton_molloy_keir;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.cs360_charlton_molloy_keir.databinding.FragmentSecondBinding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SecondFragment extends Fragment {

    // Small tolerance for floating point comparisons
    private static final double EPSILON = 0.01;

    private FragmentSecondBinding binding;
    private AppDatabaseHelper db;
    private long userId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSecondBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = new AppDatabaseHelper(requireContext());
        userId = Session.getLoggedInUserId(requireContext());

        // If no user is logged in, go back to the previous screen
        if (userId == -1) {
            ToastUtil.show(requireContext(), R.string.toast_login_first);
            NavHostFragment.findNavController(SecondFragment.this).popBackStack();
            return;
        }

        // Add MM/DD/YYYY and automatically add slashes to the date field
        attachDateMask(binding.editEntryDate);

        // Prefill date with today
        binding.editEntryDate.setText(DateUtil.getTodayDate());

        // Open SMS settings screen
        binding.buttonOpenSms.setOnClickListener(v ->
                NavHostFragment.findNavController(SecondFragment.this)
                        .navigate(R.id.action_SecondFragment_to_SmsFragment)
        );

        // Save goal weight
        binding.buttonSaveGoal.setOnClickListener(v -> onSaveGoal());

        // Add a new daily weight entry
        binding.buttonAddEntry.setOnClickListener(v -> onAddEntry());

        refreshGoalUI();
        refreshTable();
    }

    // Function to save/update the goal weight
    private void onSaveGoal() {
        String goalText = binding.editGoalWeight.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(goalText)) {
            ToastUtil.show(requireContext(), R.string.toast_enter_goal);
            return;
        }

        double goal;
        try {
            goal = Double.parseDouble(goalText);
        } catch (NumberFormatException ex) {
            ToastUtil.show(requireContext(), R.string.toast_goal_not_number);
            return;
        }

        if (goal <= 0) {
            ToastUtil.show(requireContext(), R.string.toast_goal_positive);
            return;
        }

        // Save goal to the database
        db.upsertGoalWeight(userId, goal);

        // Reset the "already notified" marker when the goal changes
        Session.clearGoalNotified(requireContext(), userId);

        binding.editGoalWeight.setText("");
        refreshGoalUI();
        ToastUtil.show(requireContext(), R.string.toast_goal_saved);
    }

    // Function to add a new weight entry
    private void onAddEntry() {
        String date = binding.editEntryDate.getText().toString().trim();
        String weightText = binding.editEntryWeight.getText().toString().trim();

        // If date is empty, use today
        if (TextUtils.isEmpty(date)) {
            date = DateUtil.getTodayDate();
            binding.editEntryDate.setText(date);
        }

        // Validate the date before saving
        if (!DateUtil.isValidDate(date)) {
            binding.editEntryDate.setError(getString(R.string.toast_date_invalid));
            ToastUtil.show(requireContext(), R.string.toast_date_invalid);
            return;
        }
        binding.editEntryDate.setError(null);

        // Validate weight input
        if (TextUtils.isEmpty(weightText)) {
            ToastUtil.show(requireContext(), R.string.toast_enter_weight);
            return;
        }

        double weight;
        try {
            weight = Double.parseDouble(weightText);
        } catch (NumberFormatException ex) {
            ToastUtil.show(requireContext(), R.string.toast_weight_not_number);
            return;
        }

        if (weight <= 0) {
            ToastUtil.show(requireContext(), R.string.toast_weight_positive);
            return;
        }

        // Insert into database
        long id = db.addDailyWeight(userId, date, weight);
        if (id == -1) {
            ToastUtil.show(requireContext(), R.string.toast_entry_save_failed);
            return;
        }

        // Clear weight input and refresh table
        binding.editEntryWeight.setText("");
        refreshTable();

        // Check goal trigger after saving the entry
        checkGoalAndNotifyIfNeeded(weight);
    }

    // Function to check the goal trigger and send SMS if enabled
    private void checkGoalAndNotifyIfNeeded(double latestWeight) {
        Double goal = db.getGoalWeight(userId);

        // If no goal is set, do nothing
        if (goal == null) {
            return;
        }

        // Treat reaching the goal as "at or under the goal"
        boolean reached = latestWeight <= (goal + EPSILON);
        if (!reached) {
            return;
        }

        // Only proceed if the user enabled SMS alerts
        if (!Session.isSmsEnabled(requireContext(), userId)) {
            return;
        }

        // Prevent sending repeated texts for the same goal value
        if (Session.wasGoalAlreadyNotified(requireContext(), userId, goal)) {
            return;
        }

        String phoneNumber = Session.getSmsPhoneNumber(requireContext(), userId);

        // If no phone number is saved, do not attempt to send
        if (TextUtils.isEmpty(phoneNumber)) {
            ToastUtil.show(requireContext(), R.string.toast_sms_not_sent);
            return;
        }

        String message = getString(R.string.sms_goal_reached_message, formatWeight(latestWeight));

        try {
            boolean attempted = SmsUtil.trySendSms(requireContext(), phoneNumber, message);

            if (attempted) {
                Session.markGoalNotified(requireContext(), userId, goal);
                ToastUtil.show(requireContext(), R.string.toast_sms_sent);
            } else {
                ToastUtil.show(requireContext(), R.string.toast_sms_not_sent);
            }
        } catch (Exception ex) {
            ToastUtil.show(requireContext(), R.string.toast_sms_failed);
        }
    }

    // Function to refresh the goal display text
    private void refreshGoalUI() {
        Double goal = db.getGoalWeight(userId);

        if (goal == null) {
            binding.textCurrentGoal.setText(R.string.current_goal_not_set);
        } else {
            binding.textCurrentGoal.setText(getString(
                    R.string.current_goal_value,
                    formatWeight(goal)
            ));
        }
    }

    // Function to display all entries in the table (date + weight + actions)
    private void refreshTable() {
        TableLayout table = binding.tableWeights;

        // Keep header row (index 0) and clear everything below it
        while (table.getChildCount() > 1) {
            table.removeViewAt(1);
        }

        List<WeightEntry> entries = db.getDailyWeights(userId);

        // Sort by actual date (newest first), while still displaying MM/DD/YYYY
        sortEntriesByDate(entries);

        for (WeightEntry entry : entries) {
            table.addView(buildDataRow(entry));
        }

        if (entries.isEmpty()) {
            table.addView(buildEmptyRow());
        }
    }

    // Function to build the "no entries" row
    private TableRow buildEmptyRow() {
        TableRow row = new TableRow(requireContext());
        row.setPadding(0, 16, 0, 16);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView tv = new TextView(requireContext());
        tv.setText(R.string.empty_entries_message);

        // Span across all 3 columns so it doesn't look misaligned
        TableRow.LayoutParams lp = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
        );
        lp.span = 3;
        tv.setLayoutParams(lp);

        row.addView(tv);
        return row;
    }

    // Function to build a data row with Edit and Delete actions
    private TableRow buildDataRow(WeightEntry entry) {
        TableRow row = new TableRow(requireContext());
        row.setPadding(0, 12, 0, 12);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView dateTv = new TextView(requireContext());
        dateTv.setText(entry.date);
        dateTv.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2f));
        dateTv.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);

        TextView weightTv = new TextView(requireContext());
        weightTv.setText(formatWeight(entry.weight));
        weightTv.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1.5f));
        weightTv.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

        LinearLayout actions = new LinearLayout(requireContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

        // Give the actions column more space so buttons don't wrap
        actions.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2.5f));

        Button editBtn = new Button(requireContext());
        editBtn.setText(R.string.action_edit);

        // Reduce default Material "all caps" width so it fits better in a table
        editBtn.setAllCaps(false);
        editBtn.setMinWidth(0);
        editBtn.setMinimumWidth(0);

        editBtn.setOnClickListener(v -> showEditDialog(entry));

        Button deleteBtn = new Button(requireContext());
        deleteBtn.setText(R.string.action_delete);

        // Reduce default Material "all caps" width so it fits better in a table
        deleteBtn.setAllCaps(false);
        deleteBtn.setMinWidth(0);
        deleteBtn.setMinimumWidth(0);

        deleteBtn.setOnClickListener(v -> {

            // Delete this entry for the current user
            boolean ok = db.deleteDailyWeight(userId, entry.id);

            if (ok) {
                ToastUtil.show(requireContext(), R.string.toast_entry_deleted);
                refreshTable();
            } else {
                ToastUtil.show(requireContext(), R.string.toast_entry_delete_failed);
            }
        });

        // Small spacing between buttons
        LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        editLp.setMarginEnd(12);
        editBtn.setLayoutParams(editLp);

        actions.addView(editBtn);
        actions.addView(deleteBtn);

        row.addView(dateTv);
        row.addView(weightTv);
        row.addView(actions);
        return row;
    }

    // Function to show a dialog for editing an entry
    private void showEditDialog(WeightEntry entry) {
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(48, 24, 48, 0);

        EditText dateInput = new EditText(requireContext());
        dateInput.setHint(getString(R.string.hint_entry_date));
        dateInput.setText(entry.date);

        // Add MM/DD/YYYY and automatically add slashes to the edit dialog date field
        attachDateMask(dateInput);

        EditText weightInput = new EditText(requireContext());
        weightInput.setHint(getString(R.string.hint_entry_weight));
        weightInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        weightInput.setText(formatWeight(entry.weight));

        container.addView(dateInput);
        container.addView(weightInput);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_edit_entry_title)
                .setView(container)
                .setPositiveButton(R.string.action_save, null)
                .setNegativeButton(R.string.action_cancel, null)
                .create();

        // Keep the dialog open if validation fails
        dialog.setOnShowListener(d -> {
            Button saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveBtn.setOnClickListener(v -> {
                String newDate = dateInput.getText().toString().trim();
                String newWeightText = weightInput.getText().toString().trim();

                if (TextUtils.isEmpty(newDate) || TextUtils.isEmpty(newWeightText)) {
                    ToastUtil.show(requireContext(), R.string.toast_date_weight_required);
                    return;
                }

                // Validate the date before saving
                if (!DateUtil.isValidDate(newDate)) {
                    dateInput.setError(getString(R.string.toast_date_invalid));
                    ToastUtil.show(requireContext(), R.string.toast_date_invalid);
                    return;
                }
                dateInput.setError(null);

                double newWeight;
                try {
                    newWeight = Double.parseDouble(newWeightText);
                } catch (NumberFormatException ex) {
                    ToastUtil.show(requireContext(), R.string.toast_weight_not_number);
                    return;
                }

                if (newWeight <= 0) {
                    ToastUtil.show(requireContext(), R.string.toast_weight_positive);
                    return;
                }

                // Update this entry for the current user
                boolean ok = db.updateDailyWeight(userId, entry.id, newDate, newWeight);

                if (ok) {
                    ToastUtil.show(requireContext(), R.string.toast_entry_updated);
                    refreshTable();

                    // Check goal trigger after editing the entry
                    checkGoalAndNotifyIfNeeded(newWeight);

                    dialog.dismiss();
                } else {
                    ToastUtil.show(requireContext(), R.string.toast_entry_update_failed);
                }
            });
        });

        dialog.show();
    }

    // Function to attach a simple MM/DD/YYYY mask to an EditText
    private void attachDateMask(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {

            private boolean isUpdating = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) return;
                isUpdating = true;

                // Keep digits only
                String digits = s.toString().replaceAll("[^0-9]", "");

                // Limit to MMDDYYYY (8 digits)
                if (digits.length() > 8) {
                    digits = digits.substring(0, 8);
                }

                // Rebuild as MM/DD/YYYY
                StringBuilder out = new StringBuilder();
                for (int i = 0; i < digits.length(); i++) {
                    if (i == 2 || i == 4) out.append('/');
                    out.append(digits.charAt(i));
                }

                s.replace(0, s.length(), out.toString());

                // Keep cursor at the end
                editText.setSelection(s.length());

                isUpdating = false;
            }
        });
    }

    // Function to sort entries by date (newest first), then by id (newest first)
    private void sortEntriesByDate(List<WeightEntry> entries) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        sdf.setLenient(false);

        entries.sort((a, b) -> {
            try {
                Date da = sdf.parse(a.date);
                Date db = sdf.parse(b.date);

                if (da == null && db == null) return Long.compare(b.id, a.id);
                if (da == null) return 1;
                if (db == null) return -1;

                int cmp = db.compareTo(da); // newest date first
                if (cmp != 0) return cmp;

                return Long.compare(b.id, a.id); // tie-breaker
            } catch (Exception e) {
                return Long.compare(b.id, a.id);
            }
        });
    }

    // Function to format a weight value for display
    private String formatWeight(double weight) {
        return String.format(Locale.US, "%.1f", weight);
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