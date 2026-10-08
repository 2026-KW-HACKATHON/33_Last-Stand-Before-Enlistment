package com.discushion.identity;

import com.discushion.contracts.identity.CurrentActorProvider;
import java.util.Optional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Looks up only the authenticated subject, before a signup write transaction begins. */
public final class AuthenticatedEmailService {
    private final CurrentActorProvider actors;
    private final PrivyVerifiedEmailSource emails;

    public AuthenticatedEmailService(CurrentActorProvider actors, PrivyVerifiedEmailSource emails) {
        this.actors = actors;
        this.emails = emails;
    }

    public Optional<VerifiedEmail> currentVerifiedEmail() {
        var actor = actors.current().orElseThrow(() -> new IdentityFailure(IdentityFailure.Reason.INVALID_TOKEN));
        if (TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("Resolve provider facts before member transaction");
        return emails.find(actor.privySubject());
    }
}
