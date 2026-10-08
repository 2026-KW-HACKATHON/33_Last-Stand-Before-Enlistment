package com.discushion.login;

import com.discushion.contracts.identity.LocalMember;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/** Local signup state only. This response does not grant resource capabilities. */
record LoginResult(String registrationStatus, Member member) {
    record Member(long id, OffsetDateTime registrationCompletedAt) {}
    static LoginResult from(Optional<LocalMember> member) {
        if (member.isEmpty()) return new LoginResult("NOT_REGISTERED",null);
        var current=member.get();
        var completed=current.registrationCompletedAt().map(time->time.atOffset(ZoneOffset.ofHours(9))).orElse(null);
        return new LoginResult(completed==null?"INCOMPLETE":"COMPLETED",new Member(current.userId(),completed));
    }
}
