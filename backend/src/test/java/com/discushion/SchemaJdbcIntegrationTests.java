package com.discushion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

/** 선택 실행: localhost 시험 DB만 허용하며 원격 환경이나 application .env는 읽지 않는다. */
@EnabledIfEnvironmentVariable(named = "DISCUSHION_TEST_JDBC_URL", matches =
        "jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
class SchemaJdbcIntegrationTests {

    private Connection connect() throws Exception {
        return DriverManager.getConnection(System.getenv("DISCUSHION_TEST_JDBC_URL"),
                "postgres", System.getenv("DISCUSHION_TEST_DB_PASSWORD"));
    }

    @Test
    void realDriverReadsAllCoreTablesWithRlsEnabled() throws Exception {
        try (var connection = connect(); var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     select count(*), bool_and(relrowsecurity)
                     from pg_class where relnamespace='discushion'::regnamespace and relkind='r'
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(27);
            assertThat(result.getBoolean(2)).isTrue();
        }
    }

    @Test
    void realCatalogContainsCompositeOwnershipConstraints() throws Exception {
        try (var connection = connect(); var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     select count(*) from pg_constraint where connamespace='discushion'::regnamespace
                     and contype='f' and cardinality(conkey)>1
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(8);
        }
    }

    @Test
    void koreanTextAndAbsoluteTimeRoundTripThroughTheRealDriver() throws Exception {
        var instant = OffsetDateTime.of(2026, 10, 7, 12, 0, 0, 0, ZoneOffset.ofHours(9));
        try (var connection = connect();
             var statement = connection.prepareStatement("select ?::text, ?::timestamptz")) {
            statement.setString(1, "합성 테스트 문장");
            statement.setObject(2, instant);
            try (var result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo("합성 테스트 문장");
                assertThat(result.getObject(2, OffsetDateTime.class).toInstant()).isEqualTo(instant.toInstant());
            }
        }
    }

    @Test
    void safeIntegerBoundaryIsReadExactlyAsLong() throws Exception {
        try (var connection = connect(); var statement = connection.createStatement();
             var result = statement.executeQuery("select 9007199254740991::bigint")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getLong(1)).isEqualTo(9_007_199_254_740_991L);
        }
    }
}
