package com.discushion.share;

import com.discushion.contracts.identity.CurrentActorProvider;
import com.discushion.contracts.post.PostContextReader;
import com.discushion.contracts.share.SharedPostAccess;
import com.discushion.identity.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

final class HttpSharedPostAccess implements SharedPostAccess {
    private final Supplier<HttpServletRequest> requests;
    private final CurrentActorProvider actors;
    private final SharedGuestPostPolicy policy;
    private final PostShareTokens tokens;
    private final DataSource source;
    HttpSharedPostAccess(Supplier<HttpServletRequest> requests, CurrentActorProvider actors, PostContextReader posts, PostShareTokens tokens, DataSource source) {
        this.requests = requests; this.actors = actors; this.policy = new SharedGuestPostPolicy(actors, posts);
        this.tokens = tokens; this.source = source;
    }
    @Override public Optional<GuestContext> guestForRead(long id, Action action) { return resolve(id, action, false); }
    @Override public Optional<GuestContext> guestForWrite(long id, Action action) { return resolve(id, action, true); }
    private Optional<GuestContext> resolve(long id, Action action, boolean write) {
        if (actors.current().isPresent()) return Optional.empty();
        var request = requests.get();
        if (request.getHeader("Authorization") != null) throw new IdentityFailure(IdentityFailure.Reason.INVALID_TOKEN);
        var headers = Collections.list(request.getHeaders("X-Post-Share-Token"));
        if (headers.size() != 1) throw ShareFailure.invalidToken();
        var claim = tokens.verify(headers.get(0));
        if (!TransactionSynchronizationManager.isActualTransactionActive() || !TransactionSynchronizationManager.hasResource(source)
                || (write && TransactionSynchronizationManager.isCurrentTransactionReadOnly())) throw new IllegalStateException("Share access requires the caller's JDBC transaction");
        boolean writingAction = action == Action.CREATE_COMMENT || action == Action.CREATE_REPLY;
        if (write != writingAction) throw new IllegalArgumentException("Wrong share access transaction mode");
        try { policy.check(claim.postId(), id, action, write); }
        catch (SharedGuestPostPolicy.Denied denied) {
            throw switch (denied.reason) {
                case INVALID_ID -> new ShareFailure(400, "VALIDATION_ERROR");
                case SCOPE_MISMATCH -> new ShareFailure(403, "SHARE_SCOPE_MISMATCH");
                case UNAVAILABLE -> ShareFailure.missingPost();
                default -> ShareFailure.invalidToken();
            };
        }
        tokens.requireCurrent(claim);
        return Optional.of(new GuestContext(id, claim.expiresAt()));
    }
}
