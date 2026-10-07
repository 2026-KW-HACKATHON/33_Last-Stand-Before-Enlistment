package com.discushion.contracts.identity;

import java.util.Optional;

public interface MemberQualificationReader {
    Optional<MemberQualification> find(long userId);
}
