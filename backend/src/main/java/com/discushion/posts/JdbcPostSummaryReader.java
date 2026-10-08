package com.discushion.posts;

import com.discushion.contracts.post.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class JdbcPostSummaryReader implements PostSummaryReader {
    private final DataSource source;
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcPostSummaryReader(DataSource source) { this.source = source; jdbc = new NamedParameterJdbcTemplate(source); }
    @Override public Map<Long, PostSummary> findAll(Set<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        if (!TransactionSynchronizationManager.isActualTransactionActive() || !TransactionSynchronizationManager.hasResource(source))
            throw new IllegalStateException("Post summaries require the caller's JDBC transaction");
        if (ids.stream().anyMatch(id -> id == null || id < 1 || id > 9007199254740991L)) throw new IllegalArgumentException("Invalid post ID");
        var result = new HashMap<Long, PostSummary>();
        jdbc.query("select id,type,region_id,author_user_id,status,created_at,title,topic from discushion.posts where id in (:ids)",
                Map.of("ids", ids), rs -> {
            var status = PostStatus.valueOf(rs.getString("status")); long id = rs.getLong("id");
            result.put(id, new PostSummary(id, PostType.valueOf(rs.getString("type")), rs.getLong("region_id"),
                    rs.getLong("author_user_id"), status, rs.getTimestamp("created_at").toInstant(),
                    status == PostStatus.PUBLISHED ? Optional.of(new PostSummary.Display(rs.getString("title"), rs.getString("topic"))) : Optional.empty()));
        });
        return Map.copyOf(result);
    }
}
