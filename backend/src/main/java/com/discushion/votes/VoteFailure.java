package com.discushion.votes;

final class VoteFailure extends RuntimeException {
    final String code, field;
    final int status;

    VoteFailure(String code, int status, String field) {
        super(code);
        this.code = code;
        this.status = status;
        this.field = field;
    }

    static VoteFailure invalid(String field) {
        return new VoteFailure("VALIDATION_ERROR", 400, field);
    }
}
