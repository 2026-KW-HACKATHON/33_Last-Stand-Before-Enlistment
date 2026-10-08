package com.discushion.bookmark;

final class BookmarkInput {
    static final long MAX_ID = 9_007_199_254_740_991L;
    private BookmarkInput() {}

    static long postId(String raw) {
        if (raw == null || !raw.matches("[0-9]{1,16}")) throw BookmarkFailure.invalid("postId");
        try {
            long value = Long.parseLong(raw);
            if (value < 1 || value > MAX_ID) throw BookmarkFailure.invalid("postId");
            return value;
        } catch (NumberFormatException invalid) {
            throw BookmarkFailure.invalid("postId");
        }
    }
}
