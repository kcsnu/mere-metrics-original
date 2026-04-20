package com.example.cs360_charlton_molloy_keir;

public final class WeightEntry {

    // Database row id
    public final long id;

    // Entry date (stored as text, MM/dd/yyyy)
    public final String date;

    // Weight value (lbs)
    public final double weight;

    public WeightEntry(long id, String date, double weight) {
        this.id = id;

        // Normalize date to avoid nulls and extra whitespace
        this.date = (date == null) ? "" : date.trim();

        this.weight = weight;
    }
}