package com.example.cs360_charlton_molloy_keir;

import android.content.Context;
import android.widget.Toast;

public final class ToastUtil {

    // Private constructor to prevent creating instances of this utility class
    private ToastUtil() { }

    // Function to show a short Toast message
    public static void show(Context context, int messageResId) {
        Toast.makeText(context, context.getString(messageResId), Toast.LENGTH_SHORT).show();
    }
}