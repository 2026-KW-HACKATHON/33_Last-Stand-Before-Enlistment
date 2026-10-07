package com.discushion.contracts.identity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public record MemberQualification(long userId, Optional<Instant> registrationCompletedAt,
        Set<Long> verifiedRegionIds, List<InstitutionGrant> institutionGrants, Instant evaluatedAt) {
    public MemberQualification {
        verifiedRegionIds = Set.copyOf(verifiedRegionIds);
        institutionGrants = List.copyOf(institutionGrants);
    }
}
