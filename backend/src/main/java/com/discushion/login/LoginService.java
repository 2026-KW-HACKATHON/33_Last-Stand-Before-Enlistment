package com.discushion.login;

import com.discushion.contracts.identity.CurrentActorProvider;
import com.discushion.contracts.identity.LocalMember;
import com.discushion.identity.IdentityFailure;
import java.util.Optional;
import java.util.function.Function;

final class LoginService {
    private final CurrentActorProvider actors;
    private final Function<String,Optional<LocalMember>> members;
    LoginService(CurrentActorProvider actors,Function<String,Optional<LocalMember>> members) {
        this.actors=actors;this.members=members;
    }
    LoginResult current() {
        var actor=actors.current().orElseThrow(()->new IdentityFailure(IdentityFailure.Reason.INVALID_TOKEN));
        // A signup may have committed since the filter's membership lookup. Always read current DB state.
        return LoginResult.from(members.apply(actor.privySubject()));
    }
}
