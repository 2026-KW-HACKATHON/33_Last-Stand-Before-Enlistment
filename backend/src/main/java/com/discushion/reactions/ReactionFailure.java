package com.discushion.reactions;

final class ReactionFailure extends RuntimeException {
    final String code, field; final int status;
    ReactionFailure(String code, int status, String field) { super(code); this.code=code; this.status=status; this.field=field; }
    static ReactionFailure invalid(String field) { return new ReactionFailure("VALIDATION_ERROR",400,field); }
}
