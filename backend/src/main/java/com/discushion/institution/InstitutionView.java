package com.discushion.institution;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;

/** Same representative-state selection and field meanings as the existing profile response. */
record InstitutionView(long id, Verification institutionVerification, boolean institutionVerified) {
    record Region(long id, String name) {}
    record Verification(String status, Long institutionId, String institutionName, Region responsibleRegion,
            OffsetDateTime completedAt, OffsetDateTime validUntil, boolean isActive) {}

    static InstitutionView from(InstitutionSnapshot snapshot) {
        var now = snapshot.evaluatedAt();
        var representative = snapshot.credentials().stream().max(
                Comparator.comparing((InstitutionSnapshot.Credential row) -> row.activeAt(now))
                        .thenComparing(InstitutionSnapshot.Credential::completedAt)
                        .thenComparingLong(InstitutionSnapshot.Credential::id));
        if (representative.isEmpty()) {
            return new InstitutionView(snapshot.userId(), new Verification("NOT_SUBMITTED", null, null, null, null, null, false), false);
        }
        var row = representative.orElseThrow();
        boolean active = row.activeAt(now);
        return new InstitutionView(snapshot.userId(), new Verification(!now.isBefore(row.validUntil()) ? "EXPIRED" : "COMPLETED",
                row.institutionId(), row.institutionName(), new Region(row.responsibleRegionId(), row.responsibleRegionName()),
                row.completedAt().atOffset(ZoneOffset.ofHours(9)), row.validUntil().atOffset(ZoneOffset.ofHours(9)), active), active);
    }
}
