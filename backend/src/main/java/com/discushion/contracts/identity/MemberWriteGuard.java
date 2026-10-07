package com.discushion.contracts.identity;

import java.util.Optional;

/** Requires the caller's active transaction; locks users before rereading qualifications. */
public interface MemberWriteGuard {
    Optional<MemberQualification> lockAndRead(long userId);
}
