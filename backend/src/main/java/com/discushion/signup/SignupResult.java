package com.discushion.signup;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

record SignupResult(String registrationStatus, Member member) {
    record Member(long id, OffsetDateTime registrationCompletedAt) {}
    static SignupResult completed(long id,Instant completed) {
        return new SignupResult("COMPLETED",new Member(id,completed.atOffset(ZoneOffset.ofHours(9))));
    }
}
