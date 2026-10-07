package com.discushion.signup;

final class SignupFailure extends RuntimeException {
    enum Reason {
        VALIDATION_ERROR(400), REQUIRED_AGREEMENT_MISSING(400), EMAIL_ALREADY_IN_USE(409),
        NICKNAME_ALREADY_IN_USE(409), REGION_NOT_FOUND(404);
        final int status;
        Reason(int status) { this.status=status; }
    }
    final Reason reason;
    final String field;
    SignupFailure(Reason reason, String field) { super(reason.name()); this.reason=reason; this.field=field; }
    static SignupFailure invalid(String field) { return new SignupFailure(Reason.VALIDATION_ERROR, field); }
}
