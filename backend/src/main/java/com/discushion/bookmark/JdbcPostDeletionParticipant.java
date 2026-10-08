package com.discushion.bookmark;

import com.discushion.contracts.participation.PostDeletionParticipant;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** BE1 bookmark cleanup adapter; the caller owns the post deletion transaction. */
final class JdbcPostDeletionParticipant implements PostDeletionParticipant {
    private final JdbcTemplate jdbc;
    JdbcPostDeletionParticipant(DataSource source){jdbc=new JdbcTemplate(source);}
    @Override public void removeBookmarks(long postId) {
        if(!TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Post deletion transaction is required");
        jdbc.update("delete from discushion.bookmarks where post_id=?",postId);
    }
}
