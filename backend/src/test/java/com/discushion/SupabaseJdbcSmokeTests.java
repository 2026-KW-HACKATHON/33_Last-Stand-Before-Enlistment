package com.discushion;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Explicit opt-in, SELECT-only verification of the designated development project. */
@EnabledIfEnvironmentVariable(named = "DISCUSHION_VERIFY_SUPABASE", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("supabase")
class SupabaseJdbcSmokeTests {
    @Autowired
    private DataSource dataSource;

    private java.sql.Connection connect() throws Exception {
        var pool = (HikariDataSource) dataSource;
        assertThat(pool.getJdbcUrl()).isEqualTo("jdbc:postgresql://aws-0-ap-northeast-1.pooler.supabase.com:6543/postgres?sslmode=verify-full&sslrootcert=../.local-db/supabase-prod-ca-2021.crt&prepareThreshold=0&connectTimeout=10&socketTimeout=30");
        assertThat(pool.getUsername()).isEqualTo("postgres.pmhmgqpyvrbbseqelpze");
        assertThat(pool.getMaximumPoolSize()).isEqualTo(1);
        return pool.getConnection();
    }

    @Test
    void springDataSourceConnectsWithCertificateAndHostnameVerification() throws Exception {
        try (var connection = connect(); var statement = connection.createStatement();
             var result = statement.executeQuery("select 1, current_database(), current_setting('server_version_num')::integer")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(1);
            assertThat(result.getString(2)).isEqualTo("postgres");
            assertThat(result.getInt(3)).isBetween(170000, 179999);
        }
    }

    @Test
    void remoteSchemaAndMigrationHistoryMatchTheValidatedSchema() throws Exception {
        try (var connection = connect(); var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     select
                       (select count(*) from pg_class where relnamespace='discushion'::regnamespace and relkind='r'),
                       (select count(*) from information_schema.columns where table_schema='discushion'),
                       (select count(*) from pg_constraint where connamespace='discushion'::regnamespace and contype='f'),
                       (select count(*) from pg_class where relnamespace='discushion'::regnamespace and relkind='r' and relrowsecurity),
                       (select string_agg(version, ',' order by version) from supabase_migrations.schema_migrations)
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(27);
            assertThat(result.getInt(2)).isEqualTo(176);
            assertThat(result.getInt(3)).isEqualTo(55);
            assertThat(result.getInt(4)).isEqualTo(27);
            assertThat(result.getString(5)).isEqualTo("20261006182228,20261007011459,20261007021128,20261007023149");
        }
    }

    @Test
    void publicApiRolesCannotReadOrWriteThePrivateSchema() throws Exception {
        try (var connection = connect(); var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     select count(*), bool_and(
                       not has_schema_privilege(r.rolname,'discushion','USAGE') and
                       not exists(select 1 from pg_tables t where t.schemaname='discushion'
                         and has_table_privilege(r.rolname,format('%I.%I',t.schemaname,t.tablename),
                           'SELECT,INSERT,UPDATE,DELETE')))
                     from pg_roles r where r.rolname in ('anon','authenticated','service_role')
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(3);
            assertThat(result.getBoolean(2)).isTrue();
        }
    }
}
