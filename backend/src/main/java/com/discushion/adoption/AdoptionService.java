package com.discushion.adoption;

import com.discushion.contracts.identity.InstitutionGrant;
import com.discushion.contracts.identity.MemberQualification;
import com.discushion.contracts.post.PostContextReader;
import com.discushion.contracts.post.PostStatus;
import com.discushion.contracts.post.PostType;
import com.discushion.identity.IdentityFailure;
import com.discushion.identity.MemberAuthorization;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;

final class AdoptionService {
    private final MemberAuthorization members;
    private final Supplier<PostContextReader> posts;
    private final JdbcAdoptionStore store;
    private final TransactionTemplate transactions;
    private final Clock clock;

    AdoptionService(MemberAuthorization members, Supplier<PostContextReader> posts, JdbcAdoptionStore store,
            TransactionTemplate transactions, Clock clock) {
        this.members = members;
        this.posts = posts;
        this.store = store;
        this.transactions = transactions;
        this.clock = clock;
    }

    AdoptionResult adopt(long postId) {
        return transactions.execute(status -> {
            MemberQualification member = members.lockCurrentCompletedMember();
            var post = lockPost(postId);
            requireAdoptablePost(post);
            InstitutionGrant grant = grantForRegion(member, post.regionId());
            var current = store.findCurrent(postId, grant.institutionId());
            if (current.isPresent()) return new AdoptionResult(current.get(), false);

            int inserted = store.insert(postId, grant.institutionId(), member.userId(), grant.credentialId(), clock.instant());
            AdoptionResponse adoption = store.findCurrent(postId, grant.institutionId())
                    .orElseThrow(() -> new IllegalStateException("Adoption insert did not produce a current relation"));
            return new AdoptionResult(adoption, inserted == 1);
        });
    }

    void cancel(long postId, long adoptionId) {
        transactions.executeWithoutResult(status -> {
            MemberQualification member = members.lockCurrentCompletedMember();
            var post = lockPost(postId);
            requireAdoptablePost(post);
            InstitutionGrant grant = grantForRegion(member, post.regionId());
            var adoption = store.findForUpdate(postId, adoptionId, grant.institutionId())
                    .orElseThrow(AdoptionFailure::adoptionNotFound);
            if (adoption.canceledAt().isPresent()) return;
            var canceledAt = clock.instant().isBefore(adoption.adoptedAt()) ? adoption.adoptedAt() : clock.instant();
            if (store.cancel(adoptionId, grant.institutionId(), member.userId(), canceledAt) != 1) {
                throw new IllegalStateException("Locked current adoption could not be canceled");
            }
        });
    }

    private com.discushion.contracts.post.PostContext lockPost(long postId) {
        var source = posts.get();
        if (source == null) throw new IllegalStateException("Post source adapter unavailable");
        var post = source.findForUpdate(postId).orElseThrow(AdoptionFailure::postNotFound);
        if (post.postId() != postId) throw new IllegalStateException("Post source returned a different target");
        return post;
    }

    private static void requireAdoptablePost(com.discushion.contracts.post.PostContext post) {
        if (post.status() != PostStatus.PUBLISHED) throw AdoptionFailure.postNotFound();
        if (post.type() != PostType.LOCAL_AGENDA) throw AdoptionFailure.notAllowed();
    }

    private InstitutionGrant grantForRegion(MemberQualification member, long regionId) {
        var now = clock.instant();
        List<InstitutionGrant> active = member.institutionGrants().stream()
                .filter(grant -> !now.isBefore(grant.completedAt()) && now.isBefore(grant.validUntil()))
                .toList();
        if (active.isEmpty()) throw new IdentityFailure(IdentityFailure.Reason.INSTITUTION_REQUIRED);
        List<InstitutionGrant> inRegion = active.stream()
                .filter(grant -> grant.responsibleRegionId() == regionId)
                .toList();
        if (inRegion.isEmpty()) throw AdoptionFailure.notAllowed();
        if (inRegion.size() > 1) throw new IllegalStateException("Multiple active institution grants match one region");
        return inRegion.get(0);
    }
}
