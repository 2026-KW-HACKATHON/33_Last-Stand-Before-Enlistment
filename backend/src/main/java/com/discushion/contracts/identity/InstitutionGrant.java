package com.discushion.contracts.identity;

import java.time.Instant;

public record InstitutionGrant(long credentialId, long institutionId, long responsibleRegionId,
        Instant completedAt, Instant validUntil) {}
