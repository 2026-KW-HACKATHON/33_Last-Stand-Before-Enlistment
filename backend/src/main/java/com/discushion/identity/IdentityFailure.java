package com.discushion.identity;

/** Internal failure reasons; HTTP code registration is a separate common contract. */
public final class IdentityFailure extends RuntimeException {
    public enum Reason { INVALID_TOKEN, NOT_REGISTERED, INCOMPLETE, PROVIDER_UNAVAILABLE,
        MEMBER_NOT_FOUND, REGION_REQUIRED, INSTITUTION_REQUIRED }
    private final Reason reason;

    public IdentityFailure(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}
