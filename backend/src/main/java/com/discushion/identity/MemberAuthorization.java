package com.discushion.identity;

import com.discushion.contracts.identity.*;
import java.time.Clock;
import java.util.Optional;
import static com.discushion.identity.IdentityFailure.Reason.*;

/** Features still check their target's type/status and ownership under their target locks. */
public final class MemberAuthorization {
    private final CurrentActorProvider actors;
    private final MemberWriteGuard guard;
    private final Clock clock;

    public MemberAuthorization(CurrentActorProvider actors, MemberWriteGuard guard, Clock clock) {
        this.actors = actors;
        this.guard = guard;
        this.clock = clock;
    }

    public MemberQualification lockCurrentCompletedMember() {
        var actor = actors.current().orElseThrow(() -> new IdentityFailure(INVALID_TOKEN));
        var member = actor.member().orElseThrow(() -> new IdentityFailure(NOT_REGISTERED));
        var fresh = guard.lockAndRead(member.userId()).orElseThrow(() -> new IdentityFailure(MEMBER_NOT_FOUND));
        if (fresh.userId() != member.userId()) throw new IllegalStateException("Member guard returned a different member");
        if (fresh.registrationCompletedAt().isEmpty()) throw new IdentityFailure(INCOMPLETE);
        return fresh;
    }

    public void requireVerifiedRegion(MemberQualification member, long regionId) {
        completed(member);
        if (!member.verifiedRegionIds().contains(regionId)) throw new IdentityFailure(REGION_REQUIRED);
    }

    public void requireActiveInstitution(MemberQualification member, Optional<Long> responsibleRegionId) {
        completed(member);
        var now = clock.instant();
        boolean active = member.institutionGrants().stream().anyMatch(grant ->
            !now.isBefore(grant.completedAt()) && now.isBefore(grant.validUntil())
                && responsibleRegionId.map(id -> id == grant.responsibleRegionId()).orElse(true));
        if (!active) throw new IdentityFailure(INSTITUTION_REQUIRED);
    }

    /** The feature maps false to its existing edit/delete error after reading the real target. */
    public boolean isOwner(MemberQualification member, long ownerUserId) {
        completed(member);
        return member.userId() == ownerUserId;
    }

    private static void completed(MemberQualification member) {
        if (member.registrationCompletedAt().isEmpty()) throw new IdentityFailure(INCOMPLETE);
    }
}
