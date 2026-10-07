package com.discushion.login;

import com.discushion.contracts.identity.LocalMember;
import com.discushion.contracts.identity.VerifiedActor;
import com.discushion.identity.IdentityFailure;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import static org.assertj.core.api.Assertions.*;

class LoginServiceTests {
    private static final String SUBJECT="did:privy:synthetic-login8-user";
    @Test void anonymousCannotReadMemberState() {
        var service=new LoginService(Optional::empty,subject->{throw new AssertionError("anonymous DB lookup");});
        assertThatThrownBy(service::current).isInstanceOfSatisfying(IdentityFailure.class,
            failure->assertThat(failure.reason()).isEqualTo(IdentityFailure.Reason.INVALID_TOKEN));
    }
    @Test void verifiedSubjectWithoutLocalMemberIsNotRegistered() {
        var seen=new AtomicReference<String>();
        var service=new LoginService(()->Optional.of(new VerifiedActor(SUBJECT,Optional.empty())),
            subject->{seen.set(subject);return Optional.empty();});
        assertThat(service.current()).isEqualTo(new LoginResult("NOT_REGISTERED",null));
        assertThat(seen.get()).isEqualTo(SUBJECT);
    }
    @Test void incompleteMemberReturnsIdAndNullCompletion() {
        var service=new LoginService(()->Optional.of(new VerifiedActor(SUBJECT,Optional.empty())),
            subject->Optional.of(new LocalMember(42,Optional.empty())));
        assertThat(service.current()).isEqualTo(new LoginResult("INCOMPLETE",new LoginResult.Member(42,null)));
    }
    @Test void completedMemberPreservesStoredTimeAndEstablishedTimezone() {
        var time=Instant.parse("2026-10-08T00:00:00.123456Z");
        var service=new LoginService(()->Optional.of(new VerifiedActor(SUBJECT,Optional.empty())),
            subject->Optional.of(new LocalMember(42,Optional.of(time))));
        assertThat(service.current()).isEqualTo(new LoginResult("COMPLETED",
            new LoginResult.Member(42,time.atOffset(ZoneOffset.ofHours(9)))));
    }
    @Test void requestSnapshotCannotReplaceFreshMembershipLookup() {
        var snapshot=new LocalMember(99,Optional.empty());
        var fresh=new LocalMember(42,Optional.of(Instant.parse("2026-10-08T00:00:00Z")));
        var service=new LoginService(()->Optional.of(new VerifiedActor(SUBJECT,Optional.of(snapshot))),subject->Optional.of(fresh));
        assertThat(service.current().member().id()).isEqualTo(42);
        assertThat(service.current().registrationStatus()).isEqualTo("COMPLETED");
    }
    @Test void databaseFailureIsNotHiddenAsUnregistered() {
        var service=new LoginService(()->Optional.of(new VerifiedActor(SUBJECT,Optional.empty())),
            subject->{throw new DataAccessResourceFailureException("synthetic-db-detail");});
        assertThatThrownBy(service::current).isInstanceOf(DataAccessResourceFailureException.class);
    }
}
