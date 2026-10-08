package com.discushion.adoption;

final class AdoptionIds {
    private static final long MAX_SAFE_INTEGER = 9_007_199_254_740_991L;

    private AdoptionIds() {}

    static long parse(String raw, String field) {
        if (raw == null || !raw.matches("[0-9]{1,16}")) throw AdoptionFailure.invalid(field);
        try {
            long value = Long.parseLong(raw);
            if (value > 0 && value <= MAX_SAFE_INTEGER) return value;
        } catch (NumberFormatException ignored) {
            // Mapped to the existing validation envelope below.
        }
        throw AdoptionFailure.invalid(field);
    }
}
