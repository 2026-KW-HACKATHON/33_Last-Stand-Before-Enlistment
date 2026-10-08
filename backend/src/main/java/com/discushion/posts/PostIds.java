package com.discushion.posts;

final class PostIds {
    private PostIds() {}
    static long parse(String value, String field) {
        try {
            if (value == null || !value.matches("[1-9][0-9]*")) throw new NumberFormatException();
            long id = Long.parseLong(value);
            if (id > 9_007_199_254_740_991L) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException invalid) {
            throw PostEditFailure.invalid(field);
        }
    }
}
