package com.discushion;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Explicit opt-in, SELECT-only verification of the designated development project. */
@EnabledIfEnvironmentVariable(named = "DISCUSHION_VERIFY_SUPABASE", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.username=${DB_AUDIT_USERNAME:${DB_USERNAME}}",
        "spring.datasource.password=${DB_AUDIT_PASSWORD:${DB_PASSWORD}}"
})
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
                       (select string_agg(version, ',' order by version) from supabase_migrations.schema_migrations),
                       (select count(*) from information_schema.columns where table_schema='discushion'
                         and table_name='media_files' and column_name in ('upload_authorization_expires_at',
                           'deletion_claim_token','deletion_claimed_at','deletion_claim_expires_at')),
                       (select count(*) from pg_constraint where connamespace='discushion'::regnamespace
                         and convalidated and conname in ('media_lifecycle_shape','media_lifecycle_times',
                           'media_upload_authorization_time','media_deletion_claim_shape',
                           'media_deleted_after_upload_authorization','post_photos_file_id_key')),
                       (select count(*) from pg_index i join pg_class c on c.oid=i.indexrelid
                         where c.relnamespace='discushion'::regnamespace and i.indisvalid and i.indisready
                           and c.relname in ('ix_media_uploading_expiry','ix_media_delete_claim_expiry')),
                       (select name from supabase_migrations.schema_migrations where version='20261008014345'),
                       (select md5(btrim(regexp_replace(regexp_replace(array_to_string(statements,chr(10)),
                           '--[^\\r\\n]*','','g'),'\\s+',' ','g')))
                         from supabase_migrations.schema_migrations where version='20261008014345'),
                       (select count(*) from pg_policies where schemaname='discushion'
                         and roles=array['discushion_server']::name[]),
                       (select count(distinct tablename) from pg_policies where schemaname='discushion'
                         and roles=array['discushion_server']::name[]),
                       (select name from supabase_migrations.schema_migrations where version='20261008111216'),
                       (select md5(btrim(regexp_replace(regexp_replace(array_to_string(statements,chr(10)),
                           '--[^\\r\\n]*','','g'),'\\s+',' ','g')))
                         from supabase_migrations.schema_migrations where version='20261008111216'),
                       (select count(*) from information_schema.columns where table_schema='discushion'
                         and table_name='media_files' and column_name in ('upload_transport','upload_attempt_id',
                           'upload_attempt_status','upload_attempt_started_at','upload_attempt_finished_at')),
                       (select count(*) from pg_constraint where conrelid='discushion.media_files'::regclass
                         and convalidated and conname in ('media_upload_transport','media_relay_shape',
                           'media_upload_attempt_shape','media_relay_deleted_safe')),
                       (select count(*) from pg_trigger where tgrelid='discushion.media_files'::regclass
                         and not tgisinternal and tgenabled='O'
                         and tgfoid='discushion.guard_photo_upload_attempt()'::regprocedure),
                       (select not prosecdef and proconfig=array['search_path=pg_catalog']::text[]
                         and has_function_privilege('discushion_server',oid,'EXECUTE')
                         and not exists(select 1 from aclexplode(coalesce(proacl,acldefault('f',proowner))) a
                           where a.grantee=0 and a.privilege_type='EXECUTE')
                         from pg_proc where oid='discushion.guard_photo_upload_attempt()'::regprocedure)
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(27);
            assertThat(result.getInt(2)).isEqualTo(185);
            assertThat(result.getInt(3)).isEqualTo(55);
            assertThat(result.getInt(4)).isEqualTo(27);
            // The remote apply recorded a different version for the reviewed #138 SQL.
            // Preserve both applied histories and verify this explicit mapping by name AND content.
            assertThat(result.getString(5)).isEqualTo("20261006182228,20261007011459,20261007021128,20261007023149,20261007104543,20261008014345,20261008111216,20261008163156,20261008163204,20261008163213,20261008163224,20261008163247,20261008163255,20261008163300,20261008163306,20261008163319,20261008163327,20261008163331");
            assertThat(result.getInt(6)).isEqualTo(4);
            assertThat(result.getInt(7)).isEqualTo(6);
            assertThat(result.getInt(8)).isEqualTo(2);
            assertThat(result.getString(9)).isEqualTo("configure_server_runtime_permissions");
            var permissionSql = Files.readString(Path.of("../supabase/migrations/20261007202633_configure_server_runtime_permissions.sql"), StandardCharsets.UTF_8);
            var normalized = permissionSql.replaceAll("--[^\\r\\n]*", "").replaceAll("\\s+", " ").strip();
            var expectedDigest = HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(normalized.getBytes(StandardCharsets.UTF_8)));
            assertThat(result.getString(10)).as("Remote #138 SQL must match the repository migration").isEqualTo(expectedDigest);
            assertThat(result.getInt(11)).isEqualTo(55);
            assertThat(result.getInt(12)).isEqualTo(21);
            // Supabase apply_migration generated this version; preserve the local applied file name.
            assertThat(result.getString(13)).isEqualTo("track_server_photo_uploads");
            var relaySql = Files.readString(Path.of("../supabase/migrations/20261008071616_track_server_photo_uploads.sql"), StandardCharsets.UTF_8);
            var normalizedRelay = relaySql.replaceAll("--[^\\r\\n]*", "").replaceAll("\\s+", " ").strip();
            var relayDigest = HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(normalizedRelay.getBytes(StandardCharsets.UTF_8)));
            assertThat(result.getString(14)).as("Remote relay SQL must match the repository migration").isEqualTo(relayDigest);
            assertThat(result.getInt(15)).isEqualTo(5);
            assertThat(result.getInt(16)).isEqualTo(4);
            assertThat(result.getInt(17)).isEqualTo(1);
            assertThat(result.getBoolean(18)).as("Relay guard must retain invoker security and least privilege").isTrue();
        }
        // MCP records application time as the remote version. Compare the permission
        // rollout by unique name and SQL. Earlier schema history stores parsed
        // statements without terminators and remains covered by the schema assertions.
        try (var connection = connect(); var statement = connection.prepareStatement("""
                select md5(btrim(regexp_replace(regexp_replace(array_to_string(statements,chr(10)),
                    '--[^\\r\\n]*','','g'),'\\s+',' ','g')))
                from supabase_migrations.schema_migrations where name=?
                """); var files = Files.list(Path.of("../supabase/migrations"))) {
            for (var file : files.filter(path -> path.toString().endsWith(".sql")
                    && path.getFileName().toString().compareTo("20261008070000") >= 0).toList()) {
                var filename = file.getFileName().toString();
                var name = filename.substring(15, filename.length() - 4);
                var normalized = Files.readString(file, StandardCharsets.UTF_8)
                        .replaceAll("--[^\\r\\n]*", "").replaceAll("\\s+", " ").strip();
                var digest = HexFormat.of().formatHex(MessageDigest.getInstance("MD5")
                        .digest(normalized.getBytes(StandardCharsets.UTF_8)));
                statement.setString(1, name);
                try (var row = statement.executeQuery()) {
                    assertThat(row.next()).as("Applied migration %s", name).isTrue();
                    assertThat(row.getString(1)).as("Canonical SQL for %s", name).isEqualTo(digest);
                    assertThat(row.next()).as("Unique applied migration %s", name).isFalse();
                }
            }
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
