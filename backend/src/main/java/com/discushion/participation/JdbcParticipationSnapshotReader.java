package com.discushion.participation;

import com.discushion.contracts.participation.ParticipationSnapshot;
import com.discushion.contracts.participation.ParticipationSnapshot.*;
import com.discushion.contracts.participation.ParticipationSnapshotReader;
import java.time.Clock;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Reads real participation relations in batch queries, within the caller's snapshot. */
public final class JdbcParticipationSnapshotReader implements ParticipationSnapshotReader {
    private final DataSource source;
    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;
    public JdbcParticipationSnapshotReader(DataSource source, Clock clock) {
        this.source = source; this.jdbc = new NamedParameterJdbcTemplate(source); this.clock = clock;
    }
    @Override public Map<Long, ParticipationSnapshot> findAll(Set<Long> ids, OptionalLong viewer) {
        if (ids.isEmpty()) return Map.of();
        if (!TransactionSynchronizationManager.isActualTransactionActive() || !TransactionSynchronizationManager.hasResource(source))
            throw new IllegalStateException("Participation requires the caller's JDBC transaction");
        if (ids.stream().anyMatch(id -> id == null || id < 1 || id > 9007199254740991L)
                || (viewer.isPresent() && (viewer.getAsLong() < 1 || viewer.getAsLong() > 9007199254740991L)))
            throw new IllegalArgumentException("Invalid internal identity");
        var args = new HashMap<String, Object>(); args.put("ids", ids);
        var states = new LinkedHashMap<Long, State>();
        jdbc.query("""
                select p.id,p.type,poll.id poll_id from discushion.posts p
                left join discushion.polls poll on poll.post_id=p.id
                where p.id in (:ids) and p.status='PUBLISHED'
                """, args, rs -> {
            var state = new State(); state.pollId = (Long) rs.getObject("poll_id");
            if ("VOTE".equals(rs.getString("type")) != (state.pollId != null)) throw new IllegalStateException("Published post and poll disagree");
            states.put(rs.getLong("id"), state);
        });
        if (states.isEmpty()) return Map.of();
        args.put("ids", states.keySet());
        jdbc.query("""
                select post_id,count(*) filter(where parent_comment_id is null) parents,
                count(*) filter(where parent_comment_id is not null) replies
                from discushion.comments where post_id in (:ids) group by post_id
                """, args, rs -> { var s = states.get(rs.getLong(1)); s.parents = rs.getLong(2); s.replies = rs.getLong(3); });
        jdbc.query("""
                select post_id,reaction_type,count(*) from discushion.post_reactions
                where post_id in (:ids) group by post_id,reaction_type
                """, args, rs -> { states.get(rs.getLong(1)).counts.put(ReactionType.valueOf(rs.getString(2)), rs.getLong(3)); });
        jdbc.query("""
                select p.post_id,o.id,count(v.user_id) from discushion.polls p
                join discushion.poll_options o on o.poll_id=p.id
                left join discushion.vote_selections v on v.poll_id=o.poll_id and v.option_id=o.id
                where p.post_id in (:ids) group by p.post_id,o.id,o.sort_order order by p.post_id,o.sort_order,o.id
                """, args, rs -> { states.get(rs.getLong(1)).options.add(new OptionVotes(rs.getLong(2), rs.getLong(3))); });
        if (viewer.isPresent()) {
            args.put("viewer", viewer.getAsLong());
            jdbc.query("select post_id,reaction_type from discushion.post_reactions where post_id in (:ids) and user_id=:viewer",
                    args, rs -> { states.get(rs.getLong(1)).mine.add(ReactionType.valueOf(rs.getString(2))); });
            jdbc.query("select post_id from discushion.bookmarks where post_id in (:ids) and user_id=:viewer",
                    args, rs -> { states.get(rs.getLong(1)).bookmarked = true; });
            jdbc.query("""
                    select p.post_id,v.option_id from discushion.polls p join discushion.vote_selections v on v.poll_id=p.id
                    where p.post_id in (:ids) and v.user_id=:viewer
                    """, args, rs -> { states.get(rs.getLong(1)).selected = OptionalLong.of(rs.getLong(2)); });
        }
        var out = new LinkedHashMap<Long, ParticipationSnapshot>(); var now = clock.instant();
        states.forEach((id, s) -> {
            Optional<PollResult> poll = Optional.empty();
            if (s.pollId != null) {
                if (s.options.size() < 2 || s.options.size() > 10) throw new IllegalStateException("Published poll options unavailable");
                long total = s.options.stream().mapToLong(OptionVotes::count).reduce(0L, Math::addExact);
                poll = Optional.of(new PollResult(s.pollId, total, s.options));
            }
            out.put(id, new ParticipationSnapshot(id, s.parents, s.replies,
                    new ReactionCounts(s.counts.getOrDefault(ReactionType.EMPATHY, 0L), s.counts.getOrDefault(ReactionType.NEEDED, 0L),
                            s.counts.getOrDefault(ReactionType.CURIOUS, 0L)), poll,
                    viewer.isEmpty() ? Optional.empty() : Optional.of(new ViewerState(s.mine, s.bookmarked, s.selected)), now));
        });
        return Map.copyOf(out);
    }
    private static final class State {
        Long pollId; long parents, replies; boolean bookmarked;
        OptionalLong selected = OptionalLong.empty();
        final EnumMap<ReactionType, Long> counts = new EnumMap<>(ReactionType.class);
        final Set<ReactionType> mine = EnumSet.noneOf(ReactionType.class);
        final List<OptionVotes> options = new ArrayList<>();
    }
}
