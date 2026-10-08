package com.discushion.share;

import com.discushion.contracts.identity.CurrentActorProvider;
import com.discushion.contracts.post.*;
import com.discushion.identity.*;
import java.net.URI;
import org.springframework.transaction.support.TransactionTemplate;

final class ShareLinkService {
    record Link(long postId, String shareUrl) {}
    private final CurrentActorProvider actors;
    private final JdbcMemberStore members;
    private final PostContextReader posts;
    private final PostShareTokens tokens;
    private final TransactionTemplate reads;
    private final String publicBaseUrl;
    ShareLinkService(CurrentActorProvider actors, JdbcMemberStore members, PostContextReader posts,
            PostShareTokens tokens, TransactionTemplate reads, String publicBaseUrl) {
        this.actors = actors; this.members = members; this.posts = posts; this.tokens = tokens;
        this.reads = reads; this.publicBaseUrl = publicBaseUrl;
    }
    Link issue(String value) {
        var actor = actors.current().orElseThrow(() -> new IdentityFailure(IdentityFailure.Reason.INVALID_TOKEN));
        long id = PostShareTokens.validPostId(value);
        return reads.execute(status -> {
            var member = members.findByVerifiedSubject(actor.privySubject()).orElseThrow(() -> new IdentityFailure(IdentityFailure.Reason.NOT_REGISTERED));
            if (member.registrationCompletedAt().isEmpty()) throw new IdentityFailure(IdentityFailure.Reason.INCOMPLETE);
            var post = posts.find(id).orElseThrow(ShareFailure::missingPost);
            if (post.postId() != id) throw new IllegalStateException("Unexpected share target");
            if (post.status() != PostStatus.PUBLISHED) throw ShareFailure.missingPost();
            return new Link(id, webBase() + "/shared/posts/" + id + "?token=" + tokens.issue(id));
        });
    }
    private String webBase() {
        try {
            var uri = URI.create(publicBaseUrl);
            boolean localHttp = "http".equals(uri.getScheme()) && ("localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost()));
            if (!uri.isAbsolute() || uri.getHost() == null || !("https".equals(uri.getScheme()) || localHttp)
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) throw new IllegalArgumentException();
            return uri.toASCIIString().replaceAll("/+$", "");
        } catch (RuntimeException invalid) { throw new IllegalStateException("Public web configuration unavailable"); }
    }
}
