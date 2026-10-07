package com.discushion.identity;

import com.discushion.contracts.identity.*;
import com.discushion.support.ContractFixtures;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static com.discushion.identity.IdentityFailure.Reason.*;

class MemberAuthorizationTests {
    private final MemberQualification completed = ContractFixtures.member(true, Set.of(10L), List.of(ContractFixtures.institution(false)));

    private MemberAuthorization auth(boolean member, MemberQualification fresh) {
        return new MemberAuthorization(ContractFixtures.authenticated(ContractFixtures.actor(member, true)),
            ignored -> Optional.of(fresh), ContractFixtures.CLOCK);
    }

    @Test void rechecksCompletionFromGuardRatherThanAuthSnapshot() {
        var fresh = ContractFixtures.member(false, Set.of(), List.of());
        assertThatThrownBy(() -> auth(true, fresh).lockCurrentCompletedMember()).isInstanceOfSatisfying(IdentityFailure.class,
            error -> assertThat(error.reason()).isEqualTo(INCOMPLETE));
        assertThat(auth(true, completed).lockCurrentCompletedMember()).isSameAs(completed);
    }

    @Test void missingTokenAndUnregisteredMemberAreDistinctInternally() {
        var guest = new MemberAuthorization(ContractFixtures.guest(), ignored -> {throw new AssertionError();}, ContractFixtures.CLOCK);
        assertThatThrownBy(guest::lockCurrentCompletedMember).isInstanceOfSatisfying(IdentityFailure.class,
            error -> assertThat(error.reason()).isEqualTo(INVALID_TOKEN));
        assertThatThrownBy(() -> auth(false, completed).lockCurrentCompletedMember()).isInstanceOfSatisfying(IdentityFailure.class,
            error -> assertThat(error.reason()).isEqualTo(NOT_REGISTERED));
    }

    @Test void institutionDoesNotGrantNeighbourPermissionAndOwnerIsSeparate() {
        var service = auth(true, completed);
        service.requireVerifiedRegion(completed, 10);
        assertThatThrownBy(() -> service.requireVerifiedRegion(completed, 11)).isInstanceOf(IdentityFailure.class);
        assertThat(service.isOwner(completed, 1)).isTrue();
        assertThat(service.isOwner(completed, 2)).isFalse();
        service.requireActiveInstitution(completed, Optional.of(10L));
        assertThatThrownBy(() -> service.requireActiveInstitution(completed, Optional.of(11L))).isInstanceOf(IdentityFailure.class);
    }

    @Test void rechecksClockAtInstitutionExpiryRatherThanTrustingSnapshot() {
        var service = new MemberAuthorization(ContractFixtures.guest(), ignored -> Optional.empty(),
            Clock.fixed(ContractFixtures.NOW.plusSeconds(3600), ZoneOffset.UTC));
        assertThatThrownBy(() -> service.requireActiveInstitution(completed, Optional.empty()))
            .isInstanceOfSatisfying(IdentityFailure.class, error -> assertThat(error.reason()).isEqualTo(INSTITUTION_REQUIRED));
    }

    @Test void bothRegistrationFailuresGoToSignupWithExistingEnvelope() {
        for(var reason : List.of(NOT_REGISTERED, INCOMPLETE)) {
            var failure = new IdentityFailure(reason);
            assertThat(IdentityErrorResponse.status(failure)).isEqualTo(403);
            var response = IdentityErrorResponse.from(failure);
            assertThat(response.code()).isEqualTo("USER_REGISTRATION_REQUIRED");
            assertThat(response.details()).isEmpty();
            assertThat(response.traceId()).isNotBlank();
        }
        assertThat(IdentityErrorResponse.status(new IdentityFailure(PROVIDER_UNAVAILABLE))).isEqualTo(503);
    }
}
