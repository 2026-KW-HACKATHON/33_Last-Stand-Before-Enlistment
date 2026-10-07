package com.discushion.region;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="DISCUSHION_TEST_JDBC_URL", matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/discushion_migration_test(?:\\?.*)?")
class JdbcRegionCatalogIntegrationTests {
    private final DriverManagerDataSource source = new DriverManagerDataSource(System.getenv("DISCUSHION_TEST_JDBC_URL"),
        "postgres", System.getenv("DISCUSHION_TEST_DB_PASSWORD"));
    private final JdbcTemplate jdbc = new JdbcTemplate(source);
    private final JdbcRegionCatalog catalog = new JdbcRegionCatalog(source);
    private final TransactionTemplate tx = new TransactionTemplate(new DataSourceTransactionManager(source));

    private long add(String name, String mapKey) {
        return jdbc.queryForObject("insert into discushion.regions(name,map_feature_key) values (?,?) returning id", Long.class, name, mapKey);
    }
    private RegionQuery query(String q, int size, String cursor) {
        var params = new LinkedMultiValueMap<String,String>();
        params.add("q",q);params.add("size",Integer.toString(size));
        if(cursor != null) params.add("cursor",cursor);
        return RegionQuery.parse(params);
    }

    @Test void koreanOrderAndIdenticalNamesUseStableIdTieBreaks() {
        tx.executeWithoutResult(status -> {
            String suffix = "-synthetic-i9-"+UUID.randomUUID();
            long ha = add("하계"+suffix, null);
            long ga1 = add("가동"+suffix, null);
            long na = add("나동"+suffix, null);
            long ga2 = add("가동"+suffix, "synthetic-map-"+UUID.randomUUID());
            var first = catalog.list(query(suffix,2,null));
            assertThat(first.data()).extracting(Region::id).containsExactly(ga1,ga2);
            assertThat(first.meta().hasNext()).isTrue();
            var last = catalog.list(query(suffix,2,first.meta().nextCursor()));
            assertThat(last.data()).extracting(Region::id).containsExactly(na,ha);
            assertThat(last.meta()).isEqualTo(new RegionPage.Meta(null,false));
            assertThat(first.data().get(0).mapFeatureKey()).isNull();
            assertThat(first.data().get(1).mapFeatureKey()).startsWith("synthetic-map-");
            status.setRollbackOnly();
        });
    }

    @Test void literalSearchPreventsWildcardAndSqlInjectionExpansion() {
        tx.executeWithoutResult(status -> {
            String prefix = "synthetic-i9-"+UUID.randomUUID();
            long literal = add(prefix+"%_!동",null);
            add(prefix+"other동",null);
            assertThat(catalog.list(query(prefix+"%_!",20,null)).data()).extracting(Region::id).containsExactly(literal);
            assertThat(catalog.list(query("' OR true --",20,null)).data()).isEmpty();
            assertThat(catalog.list(query(prefix.toUpperCase(),20,null)).data()).hasSize(2);
            status.setRollbackOnly();
        });
    }

    @Test void normalizedNamesAndCursorUseTheSameDatabaseOrdering() {
        tx.executeWithoutResult(status -> {
            String suffix = "-synthetic-i9-"+UUID.randomUUID();
            long decomposed = add("\u1100\u1161동"+suffix,null);
            long composed = add("가동"+suffix,null);
            var first = catalog.list(query("가동"+suffix,1,null));
            assertThat(first.data()).extracting(Region::id).containsExactly(decomposed);
            assertThat(catalog.list(query("가동"+suffix,1,first.meta().nextCursor())).data())
                .extracting(Region::id).containsExactly(composed);
            status.setRollbackOnly();
        });
    }

    @Test void hundredIsAPageLimitAndAllRowsRemainReachable() {
        tx.executeWithoutResult(status -> {
            String prefix = "synthetic-i9-"+UUID.randomUUID();
            var ids = new ArrayList<Long>();
            for(int i=0;i<103;i++) ids.add(add(prefix+String.format("-%03d",i),null));
            var first = catalog.list(query(prefix,100,null));
            assertThat(first.data()).hasSize(100);
            var last = catalog.list(query(prefix,100,first.meta().nextCursor()));
            assertThat(last.data()).hasSize(3);
            var actual = new ArrayList<Long>();
            first.data().forEach(row -> actual.add(row.id()));last.data().forEach(row -> actual.add(row.id()));
            assertThat(actual).containsExactlyElementsOf(ids).doesNotHaveDuplicates();
            assertThat(last.meta().nextCursor()).isNull();
            status.setRollbackOnly();
        });
    }

    @Test void noMatchAndIdLookupDoNotGenerateFallbackRegions() {
        String unique = "synthetic-i9-"+UUID.randomUUID();
        assertThat(catalog.list(query(unique,20,null))).isEqualTo(new RegionPage(List.of(),new RegionPage.Meta(null,false)));
        assertThat(catalog.findById(Region.MAX_ID)).isEmpty();
        assertThatThrownBy(() -> catalog.findById(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> catalog.findById(Region.MAX_ID+1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void syntheticSelectOnlyRoleUsesRlsWithoutWriteOrSequenceGrants() {
        tx.executeWithoutResult(status -> {
            String marker="synthetic-i9-role-"+UUID.randomUUID();
            long id=add(marker,null);
            String role="i9_read_"+UUID.randomUUID().toString().replace("-","");
            jdbc.execute("create role "+role+" nologin nosuperuser nobypassrls");
            jdbc.execute("grant usage on schema discushion to "+role);
            jdbc.execute("grant select on discushion.regions to "+role);
            jdbc.execute("create policy "+role+" on discushion.regions for select to "+role+" using (true)");
            jdbc.execute("set local role "+role);
            assertThat(catalog.list(query(marker,20,null)).data()).extracting(Region::id).containsExactly(id);
            assertThat(catalog.findById(id)).isPresent();
            assertThat(jdbc.queryForObject("select bool_or(rolsuper or rolbypassrls) from pg_roles where rolname=current_user",Boolean.class)).isFalse();
            assertThat(jdbc.queryForObject("""
                select has_table_privilege(current_user,'discushion.regions','INSERT')
                    or has_table_privilege(current_user,'discushion.regions','UPDATE')
                    or has_table_privilege(current_user,'discushion.regions','DELETE')
                """,Boolean.class)).isFalse();
            assertThat(jdbc.queryForObject("select has_sequence_privilege(current_user,'discushion.regions_id_seq','USAGE')",Boolean.class)).isFalse();
            assertThatThrownBy(() -> jdbc.update("insert into discushion.regions(name) values (?)",marker+"-forbidden"))
                .isInstanceOf(org.springframework.dao.DataAccessException.class)
                .satisfies(error -> assertThat(((java.sql.SQLException)error.getCause()).getSQLState()).isEqualTo("42501"));
            status.setRollbackOnly();
        });
    }

    @Test void allRegionConsumersKeepTheSameForeignKeyAndSelectionDoesNotGrantAuthority() {
        tx.executeWithoutResult(status -> {
            long region = add("synthetic-i9-"+UUID.randomUUID(),null);
            String marker = "synthetic-i9-"+UUID.randomUUID();
            long user = jdbc.queryForObject("""
                insert into discushion.users(email,email_verified_at,created_at,updated_at,privy_user_id,registration_completed_at)
                values (? ,now(),now(),now(),?,now()) returning id
                """,Long.class,marker+"@example.invalid","did:privy:"+marker);
            jdbc.update("insert into discushion.profiles(user_id,nickname,activity_region_id,updated_at) values (?,?,?,now())",user,"i9"+user,region);
            catalog.list(query("synthetic-i9",20,null));
            assertThat(jdbc.queryForObject("select count(*) from discushion.neighbor_verified_regions where user_id=?",Integer.class,user)).isZero();
            jdbc.update("insert into discushion.neighbor_verified_regions(user_id,region_id,verified_at) values (?,?,now())",user,region);
            long institution = jdbc.queryForObject("insert into discushion.institutions(name,created_at) values (?,now()) returning id",Long.class,marker);
            jdbc.update("""
                insert into discushion.institution_credentials(user_id,institution_id,responsible_region_id,completed_at,valid_until)
                values (?,?,?,now(),now()+interval '1 year')
                """,user,institution,region);
            long post = jdbc.queryForObject("""
                insert into discushion.posts(author_user_id,region_id,type,topic,title,content,status,content_revision,created_at,updated_at)
                values (?,?,'LOCAL_AGENDA','OTHER','synthetic-i9','synthetic-i9','PUBLISHED',1,now(),now()) returning id
                """,Long.class,user,region);
            assertThat(catalog.findById(region).orElseThrow().id()).isEqualTo(region);
            assertThat(jdbc.queryForObject("""
                select count(*) from discushion.profiles p
                join discushion.neighbor_verified_regions n on n.user_id=p.user_id and n.region_id=p.activity_region_id
                join discushion.institution_credentials c on c.user_id=p.user_id and c.responsible_region_id=p.activity_region_id
                join discushion.posts b on b.author_user_id=p.user_id and b.region_id=p.activity_region_id
                where p.user_id=? and b.id=?
                """,Integer.class,user,post)).isEqualTo(1);
            assertThatThrownBy(() -> jdbc.update("update discushion.profiles set activity_region_id=? where user_id=?",Region.MAX_ID,user))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            status.setRollbackOnly();
        });
    }
}
