package com.example.cs360_charlton_molloy_keir;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;

import androidx.core.content.ContextCompat;

public final class SmsUtil {

    // Keep messages short to reduce the chance of splitting into multiple SMS parts
    private static final int MAX_SMS_CHARS = 140;

    // Minimum and maximum digits allowed in a destination number
    private static final int MIN_PHONE_DIGITS = 10;
    private static final int MAX_PHONE_DIGITS = 15;

    // Private constructor to prevent creating instances of this utility class
    private SmsUtil() { }

    // Function to check if SEND_SMS permission is granted
    public static boolean hasSendSmsPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    // Function to sanitize a phone number (digits only, allow leading '+')
    public static String sanitizePhoneNumber(String raw) {
        if (raw == null) {
            return "";
        }

        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // Keep digits and an optional leading '+'
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);

            if (Character.isDigit(c)) {
                sb.append(c);
            } else if (c == '+' && sb.length() == 0) {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    // Function to validate the destination number (10–15 digits, optional leading '+')
    public static boolean isValidDestinationPhoneNumber(String sanitizedNumber) {
        if (sanitizedNumber == null) {
            return false;
        }

        String s = sanitizedNumber.trim();
        if (s.isEmpty()) {
            return false;
        }

        int digitCount = 0;

        // Count digits and allow '+' only at the start
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (Character.isDigit(c)) {
                digitCount++;
            } else if (c == '+' && i == 0) {
                // allowed
            } else {
                return false;
            }
        }

        return digitCount >= MIN_PHONE_DIGITS && digitCount <= MAX_PHONE_DIGITS;
    }

    // Function to attempt sending a single SMS message
    // Returns true only if sendTextMessage() was actually called successfully
    public static boolean trySendSms(Context context, String phoneNumber, String message) {

        // If permission is not granted, do not send
        if (!hasSendSmsPermission(context)) {
            return false;
        }

        // Sanitize and validate the destination number
        String sanitizedNumber = sanitizePhoneNumber(phoneNumber);
        if (!isValidDestinationPhoneNumber(sanitizedNumber)) {
            return false;
        }

        // Do not send empty messages
        String safeMessage = (message == null) ? "" : message.trim();
        if (safeMessage.isEmpty()) {
            return false;
        }

        // Trim message length to keep it short
        if (safeMessage.length() > MAX_SMS_CHARS) {
            safeMessage = safeMessage.substring(0, MAX_SMS_CHARS);
        }

        try {
            // Attempt to send the SMS
            SmsManager smsManager = context.getSystemService(SmsManager.class);

            if (smsManager == null) {
                return false;
            }

            smsManager.sendTextMessage(sanitizedNumber, null, safeMessage, null, null);
            return true;
        } catch (Exception e) {
            // If sending fails for any reason, do not crash the app
            return false;
        }
    }
}