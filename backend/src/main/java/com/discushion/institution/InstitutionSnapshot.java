package com.discushion.institution;

import java.time.Instant;
import java.util.List;

/** Internal stored facts, not a public API DTO or a choice of representative credential. */
record InstitutionSnapshot(long userId, Instant evaluatedAt, List<Credential> credentials) {
    InstitutionSnapshot {
        credentials = List.copyOf(credentials);
    }

    boolean hasActiveInstitution() {
        return credentials.stream().anyMatch(credential -> credential.activeAt(evaluatedAt));
    }

    /** Qualification only; adoption still requires its own current target and ownership checks. */
    boolean responsibleFor(long regionId) {
        return credentials.stream().anyMatch(credential ->
                credential.responsibleRegionId() == regionId && credential.activeAt(evaluatedAt));
    }

    record Credential(long id, long institutionId, String institutionName,
            long responsibleRegionId, String responsibleRegionName, Instant completedAt, Instant validUntil) {
        boolean activeAt(Instant now) {
            return !now.isBefore(completedAt) && now.isBefore(validUntil);
        }
    }
}
