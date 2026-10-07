package com.discushion.identity;

import com.discushion.contracts.identity.*;
import java.time.Clock;
import java.time.Instant;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.LinkedHashSet;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Uses the shared JDBC transaction. Never links by email or creates members on login. */
public class JdbcMemberStore implements MemberQualificationReader, MemberWriteGuard {
    private static final long MAX_ID = 9007199254740991L;
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    private final Clock clock;

    public JdbcMemberStore(DataSource dataSource, Clock clock) {
        this.dataSource = dataSource;
        this.jdbc = new JdbcTemplate(dataSource);
        this.clock = clock;
    }

    public Optional<LocalMember> findByVerifiedSubject(String subject) {
        if (subject == null || subject.isBlank()) throw new IllegalArgumentException("Missing verified subject");
        return jdbc.query("select id, registration_completed_at from discushion.users where privy_user_id = ?",
            (rs, row) -> new LocalMember(rs.getLong("id"), time(rs, "registration_completed_at")), subject)
            .stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MemberQualification> find(long userId) {
        validId(userId);
        return jdbc.query("select registration_completed_at from discushion.users where id = ?",
            (rs, row) -> time(rs, "registration_completed_at"), userId)
            .stream().findFirst().map(completed -> readQualification(userId, completed));
    }

    @Override
    public Optional<MemberQualification> lockAndRead(long userId) {
        validId(userId);
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.hasResource(dataSource)
                || TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            throw new IllegalStateException("Member guard requires the caller's writable JDBC transaction");
        }
        return jdbc.query("select registration_completed_at from discushion.users where id = ? for update",
            (rs, row) -> time(rs, "registration_completed_at"), userId)
            .stream().findFirst().map(completed -> readQualification(userId, completed));
    }

    private MemberQualification readQualification(long userId, Optional<Instant> completed) {
        var regions = new LinkedHashSet<>(jdbc.query(
            "select region_id from discushion.neighbor_verified_regions where user_id = ? order by region_id",
            (rs, row) -> rs.getLong("region_id"), userId));
        var grants = jdbc.query("""
            select id, institution_id, responsible_region_id, completed_at, valid_until
            from discushion.institution_credentials where user_id = ? order by id
            """, (rs, row) -> new InstitutionGrant(rs.getLong("id"), rs.getLong("institution_id"),
                rs.getLong("responsible_region_id"), time(rs, "completed_at").orElseThrow(),
                time(rs, "valid_until").orElseThrow()), userId);
        return new MemberQualification(userId, completed, regions, grants, clock.instant());
    }

    private static Optional<Instant> time(ResultSet rs, String column) throws SQLException {
        var value = rs.getTimestamp(column);
        return value == null ? Optional.empty() : Optional.of(value.toInstant());
    }

    private static void validId(long id) {
        if (id < 1 || id > MAX_ID) throw new IllegalArgumentException("Invalid internal member ID");
    }
}
