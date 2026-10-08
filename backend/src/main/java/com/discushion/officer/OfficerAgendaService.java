package com.discushion.officer;

import com.discushion.contracts.identity.CurrentActorProvider;
import com.discushion.contracts.identity.MemberQualificationReader;
import com.discushion.contracts.participation.ParticipationSnapshotReader;
import com.discushion.contracts.post.PostStatus;
import com.discushion.contracts.post.PostSummaryReader;
import com.discushion.contracts.post.PostType;
import com.discushion.identity.IdentityFailure;
import com.discushion.identity.MemberAuthorization;
import java.time.Clock;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

final class OfficerAgendaService {
    private final CurrentActorProvider actors;
    private final MemberQualificationReader qualifications;
    private final MemberAuthorization authorization;
    private final Supplier<PostSummaryReader> summaries;
    private final Supplier<ParticipationSnapshotReader> participation;
    private final JdbcOfficerAgendaStore store;
    private final TransactionTemplate reads;
    private final Clock clock;

    OfficerAgendaService(CurrentActorProvider actors, MemberQualificationReader qualifications,
            MemberAuthorization authorization, Supplier<PostSummaryReader> summaries,
            Supplier<ParticipationSnapshotReader> participation, JdbcOfficerAgendaStore store,
            TransactionTemplate reads, Clock clock) {
        this.actors = actors;
        this.qualifications = qualifications;
        this.authorization = authorization;
        this.summaries = summaries;
        this.participation = participation;
        this.store = store;
        this.reads = reads;
        this.clock = clock;
    }

    OfficerAgendaPage list(OfficerAgendaQuery query) {
        return reads.execute(status -> {
            var actor = actors.current().orElseThrow(() -> new IdentityFailure(IdentityFailure.Reason.INVALID_TOKEN));
            var member = actor.member().orElseThrow(() -> new IdentityFailure(IdentityFailure.Reason.NOT_REGISTERED));
            var qualification = qualifications.find(member.userId()).orElseThrow(() ->
                    new IdentityFailure(IdentityFailure.Reason.MEMBER_NOT_FOUND));
            if (qualification.registrationCompletedAt().isEmpty()) {
                throw new IdentityFailure(IdentityFailure.Reason.INCOMPLETE);
            }
            authorization.requireActiveInstitution(qualification, Optional.empty());
            var now = clock.instant();
            var active = qualification.institutionGrants().stream()
                    .filter(grant -> !now.isBefore(grant.completedAt()) && now.isBefore(grant.validUntil()))
                    .toList();
            if (active.size() != 1) {
                throw new IllegalStateException("Expected one active institution credential per user");
            }
            var credential = active.get(0);
            long institutionId = credential.institutionId();
            var filter = new OfficerAgendaCursor.Filter(institutionId, query.scope(), query.regionId());
            var after = query.cursor() == null ? null : OfficerAgendaCursor.decode(query.cursor(), filter);
            return readPage(query, filter, after, credential.responsibleRegionId());
        });
    }

    private OfficerAgendaPage readPage(OfficerAgendaQuery query, OfficerAgendaCursor.Filter filter,
            OfficerAgendaCursor.Position after, long responsibleRegionId) {
        var rows = store.page(filter.institutionId(), query, after, query.size() + 1);
        boolean hasNext = rows.size() > query.size();
        var pageRows = hasNext ? rows.subList(0, query.size()) : rows;
        if (pageRows.isEmpty()) return new OfficerAgendaPage(List.of(), new OfficerAgendaPage.Meta(null, false));

        var ids = new LinkedHashSet<Long>();
        pageRows.forEach(row -> ids.add(row.postId()));
        var postSource = summaries.get();
        var participationSource = participation.get();
        if (postSource == null || participationSource == null) {
            throw new IllegalStateException("Required post/participation source adapter unavailable");
        }
        var postSummaries = postSource.findAll(Set.copyOf(ids));
        var snapshots = participationSource.findAll(Set.copyOf(ids), OptionalLong.empty());
        if (postSummaries == null || snapshots == null) {
            throw new IllegalStateException("Officer agenda batch source returned null");
        }

        var items = new ArrayList<OfficerAgendaPage.Item>();
        for (var row : pageRows) {
            var post = postSummaries.get(row.postId());
            var snapshot = snapshots.get(row.postId());
            if (post == null || snapshot == null) {
                throw new IllegalStateException("Published agenda missing required shared snapshot");
            }
            if (post.status() != PostStatus.PUBLISHED || post.type() != PostType.LOCAL_AGENDA
                    || post.regionId() != row.regionId()) {
                throw new IllegalStateException("Officer agenda sources disagree on the published post");
            }
            var display = post.display().orElseThrow(() -> new IllegalStateException("Published post has no display"));
            var reactions = snapshot.reactions();
            long total = Math.addExact(Math.addExact(reactions.empathy(), reactions.needed()), reactions.curious());
            if (total != row.reactionCount()) {
                throw new IllegalStateException("Officer agenda reaction count sources disagree");
            }
            var adoption = row.adoptionId().isPresent()
                    ? new OfficerAgendaPage.InstitutionAdoption(row.adoptionId().getAsLong(),
                            row.adoptedAt().orElseThrow(() -> new IllegalStateException("Adoption timestamp missing")))
                    : null;
            boolean responsible = row.regionId() == responsibleRegionId;
            var capabilities = new OfficerAgendaPage.Capabilities(responsible && adoption == null,
                    responsible && adoption != null);
            items.add(new OfficerAgendaPage.Item(row.postId(), PostType.LOCAL_AGENDA, row.regionId(), row.regionName(),
                    display.title(), display.topic(), new OfficerAgendaPage.ReactionCounts(reactions.empathy(),
                            reactions.needed(), reactions.curious(), total),
                    Math.addExact(snapshot.parentCommentCount(), snapshot.replyCount()), post.createdAt(), adoption, capabilities));
        }
        String nextCursor = hasNext
                ? OfficerAgendaCursor.encode(filter, position(pageRows.get(pageRows.size() - 1))) : null;
        return new OfficerAgendaPage(items, new OfficerAgendaPage.Meta(nextCursor, hasNext));
    }

    private static OfficerAgendaCursor.Position position(JdbcOfficerAgendaStore.Row row) {
        return new OfficerAgendaCursor.Position(row.reactionCount(), row.createdAt(), row.postId());
    }
}
