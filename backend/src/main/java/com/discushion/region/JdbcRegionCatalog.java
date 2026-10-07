package com.discushion.region;

import java.util.ArrayList;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/** SELECT-only adapter on the shared regions source. No schema, seeds or eligibility writes. */
public final class JdbcRegionCatalog {
    private final JdbcTemplate jdbc;

    public JdbcRegionCatalog(DataSource source) { jdbc = new JdbcTemplate(source); }

    RegionPage list(RegionQuery query) {
        var sql = new StringBuilder("""
            select id, name, map_feature_key from discushion.regions
            where normalize(name, NFC) ilike ? escape '!'
            """);
        var args = new ArrayList<Object>();
        args.add("%" + query.q().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
        if (query.after() != null) {
            sql.append(" and (normalize(name, NFC) collate \"C\", id) > (? collate \"C\", ?)");
            args.add(query.after().name());
            args.add(query.after().id());
        }
        sql.append(" order by normalize(name, NFC) collate \"C\", id limit ?");
        args.add(query.size() + 1);
        var rows = jdbc.query(sql.toString(), (rs, index) ->
            new Region(rs.getLong("id"), rs.getString("name"), rs.getString("map_feature_key")), args.toArray());
        boolean hasNext = rows.size() > query.size();
        var data = hasNext ? rows.subList(0, query.size()) : rows;
        String cursor = hasNext ? RegionCursor.encode(query.q(), data.get(data.size() - 1)) : null;
        return new RegionPage(data, new RegionPage.Meta(cursor, hasNext));
    }

    /** For A-area signup/profile validation: a shared source ID, not a grant of neighbor authority. */
    public Optional<Region> findById(long id) {
        if (id < 1 || id > Region.MAX_ID) throw new IllegalArgumentException("Invalid Region ID");
        return jdbc.query("select id,name,map_feature_key from discushion.regions where id=?",
            (rs, index) -> new Region(rs.getLong(1), rs.getString(2), rs.getString(3)), id).stream().findFirst();
    }
}
