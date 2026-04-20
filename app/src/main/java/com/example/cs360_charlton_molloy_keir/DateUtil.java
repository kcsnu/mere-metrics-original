package com.example.cs360_charlton_molloy_keir;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DateUtil {

    // Date format used by the app (MM/dd/yyyy)
    private static final String DATE_PATTERN = "MM/dd/yyyy";

    // Private constructor to prevent creating instances of this utility class
    private DateUtil() { }

    // Function to get today's date in MM/dd/yyyy format
    public static String getTodayDate() {
        return new SimpleDateFormat(DATE_PATTERN, Locale.US).format(new Date());
    }

    // Function to validate a date string in MM/dd/yyyy format
    public static boolean isValidDate(String dateText) {
        if (dateText == null || dateText.length() != 10) {
            return false;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN, Locale.US);
        sdf.setLenient(false);

        try {
            Date parsed = sdf.parse(dateText);
            return parsed != null;
        } catch (ParseException e) {
            return false;
        }
    }
}