package com.discushion.signup;

import com.discushion.contracts.identity.CurrentActorProvider;
import com.discushion.identity.IdentityFailure;
import com.discushion.identity.VerifiedEmail;
import java.time.Clock;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;
import static com.discushion.signup.SignupFailure.Reason.*;

final class SignupService {
    record Outcome(boolean completedNow,SignupResult result) {}
    private final CurrentActorProvider actors;
    private final Supplier<Optional<VerifiedEmail>> emails;
    private final JdbcSignupStore store;
    private final TransactionTemplate transactions;
    private final Clock clock;
    SignupService(CurrentActorProvider actors,Supplier<Optional<VerifiedEmail>> emails,
                  JdbcSignupStore store,TransactionTemplate transactions,Clock clock) {
        this.actors=actors;this.emails=emails;this.store=store;this.transactions=transactions;this.clock=clock;
    }
    Outcome complete(SignupInput input) {
        var actor=actors.current().orElseThrow(()->new IdentityFailure(IdentityFailure.Reason.INVALID_TOKEN));
        if(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Resolve signup provider facts before the write transaction");
        input.validate();
        var completed=transactions.execute(status->store.lock(actor.privySubject()).filter(member->member.completed()!=null));
        if (completed.isPresent()) return new Outcome(false,SignupResult.completed(completed.get().id(),completed.get().completed()));
        var email=emails.get().orElseThrow(()->SignupFailure.invalid("email"));
        if (!actor.privySubject().equals(email.privySubject())) throw new IllegalStateException("Provider subject mismatch");
        try {
            return transactions.execute(status->{
                var existing=store.lock(actor.privySubject());
                if (existing.isEmpty()) store.insertIfAbsent(email,clock.instant());
                var member=store.lock(actor.privySubject()).orElseThrow();
                if (member.completed()!=null) return new Outcome(false,SignupResult.completed(member.id(),member.completed()));
                if (!member.email().equals(email.address())) throw SignupFailure.invalid("email");
                var now=clock.instant();
                store.save(member.id(),input,email,now);
                // Return the persisted timestamp precision, including on the first successful response.
                var saved=store.lock(actor.privySubject()).orElseThrow();
                return new Outcome(true,SignupResult.completed(saved.id(),saved.completed()));
            });
        } catch(DataIntegrityViolationException error) {
            // Inspect SQL constraint metadata only. Never return the SQL, values or raw provider/DB errors.
            for(Throwable cause=error;cause!=null;cause=cause.getCause()) {
                if(cause instanceof java.sql.SQLException sql) {
                    String detail=sql.getMessage();
                    if("23505".equals(sql.getSQLState()) && detail!=null) {
                        if(detail.contains("users_email_key")) {
                            // Concurrent inserts can hit the email index before the subject conflict target.
                            // The failed transaction has rolled back: recover only a completed, verified subject.
                            var winner=transactions.execute(status->store.lock(actor.privySubject())
                                .filter(member->member.completed()!=null));
                            if(winner.isPresent()) return new Outcome(false,
                                SignupResult.completed(winner.get().id(),winner.get().completed()));
                            throw new SignupFailure(EMAIL_ALREADY_IN_USE,"email");
                        }
                        if(detail.contains("profiles_nickname_key")) throw new SignupFailure(NICKNAME_ALREADY_IN_USE,"profile.nickname");
                    }
                    if("23503".equals(sql.getSQLState()) && detail!=null && detail.contains("profiles_activity_region_id_fkey"))
                        throw new SignupFailure(REGION_NOT_FOUND,"profile.activityRegionId");
                }
            }
            throw error;
        }
    }
}
