package com.discushion.identity;

import java.time.Instant;

/** Internal provider-confirmed facts for signup. Never a local membership/region/institution grant. */
public record VerifiedEmail(String privySubject, String address, Instant verifiedAt) {
    @Override public String toString() { return "VerifiedEmail[redacted]"; }
}
