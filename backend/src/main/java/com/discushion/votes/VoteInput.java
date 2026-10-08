package com.discushion.votes;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

record VoteInput(long postId, long optionId, boolean confirmChange) {
    private static final long MAX_SAFE_ID = 9_007_199_254_740_991L;
    private static final Set<String> FIELDS = Set.of("optionId", "confirmChange");

    static VoteInput parse(String rawPostId, Map<String, Object> body) {
        long postId = id(rawPostId, "postId");
        if (body == null || !body.keySet().equals(FIELDS)) throw VoteFailure.invalid("body");
        long optionId = jsonId(body.get("optionId"), "optionId");
        Object confirmation = body.get("confirmChange");
        if (!(confirmation instanceof Boolean confirmChange)) throw VoteFailure.invalid("confirmChange");
        return new VoteInput(postId, optionId, confirmChange);
    }

    private static long jsonId(Object value, String field) {
        if (!(value instanceof Number number)) throw VoteFailure.invalid(field);
        try {
            long id = new BigDecimal(number.toString()).longValueExact();
            if (id < 1 || id > MAX_SAFE_ID) throw VoteFailure.invalid(field);
            return id;
        } catch (NumberFormatException | ArithmeticException failure) {
            throw VoteFailure.invalid(field);
        }
    }

    private static long id(String value, String field) {
        if (value == null || !value.matches("[1-9][0-9]{0,15}")) throw VoteFailure.invalid(field);
        try {
            long id = Long.parseLong(value);
            if (id > MAX_SAFE_ID) throw VoteFailure.invalid(field);
            return id;
        } catch (NumberFormatException failure) {
            throw VoteFailure.invalid(field);
        }
    }
}
