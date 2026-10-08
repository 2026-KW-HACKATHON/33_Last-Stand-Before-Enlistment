package com.discushion.demo;

import com.discushion.neighbor.NeighborDemoProvisioner;
import com.discushion.institution.InstitutionDemoProvisioner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import tools.jackson.databind.json.JsonMapper;

/** Separate Gradle source set: never a web endpoint or part of the production bootJar. */
public final class DemoProvisionMain {
    public record Entry(String kind, long memberId, long regionId, Long institutionId, String completedAt) {}
    public record Plan(List<Entry> entries) {}
    public static void validate(Plan plan) {
        if (plan == null || plan.entries() == null || plan.entries().isEmpty()) throw new IllegalArgumentException("A nonempty explicit demo plan is required");
        var members = new HashSet<Long>();
        for (var row : plan.entries()) {
            if (row == null || !List.of("unverified", "neighbor", "institution").contains(row.kind())) throw new IllegalArgumentException("Unknown demo kind");
            validId(row.memberId()); validId(row.regionId());
            if (!members.add(row.memberId())) throw new IllegalArgumentException("Use a distinct member per demo scenario");
            if ("institution".equals(row.kind())) {
                if (row.institutionId() == null) throw new IllegalArgumentException("Missing registered institution ID");
                validId(row.institutionId()); Instant.parse(row.completedAt());
            } else if (row.institutionId() != null || row.completedAt() != null) throw new IllegalArgumentException("Unexpected institution fields");
        }
    }
    private static void validId(long value) {
        if (value < 1 || value > 9007199254740991L) throw new IllegalArgumentException("Expected actual numeric server ID");
    }
    private static String required(String name) {
        var value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + name);
        return value;
    }
    public static void main(String[] args) {
        try { run(args); }
        catch (Exception failure) { System.err.println("Demo preparation failed. Check plan, registered IDs, admin credentials and TLS. No credentials or database diagnostics are logged."); System.exit(1); }
    }
    static void run(String[] args) throws Exception {
        if (args.length != 2 || !List.of("--check", "--apply").contains(args[0])) throw new IllegalArgumentException("Use --check/--apply and an external plan path");
        var plan = JsonMapper.builder().build().readValue(Files.readString(Path.of(args[1])), Plan.class);
        validate(plan);
        var url = required("DEMO_DB_URL");
        boolean loopback = url.startsWith("jdbc:postgresql://127.0.0.1:") || url.startsWith("jdbc:postgresql://localhost:");
        if (!loopback && (!url.contains("sslmode=verify-full") || !url.contains("sslrootcert="))) throw new IllegalArgumentException("Remote DB requires verified TLS and its official CA");
        var source = new DriverManagerDataSource(url, required("DEMO_DB_USERNAME"), required("DEMO_DB_PASSWORD"));
        source.setDriverClassName("org.postgresql.Driver");
        var jdbc = new JdbcTemplate(source);
        var role = jdbc.queryForObject("select current_user", String.class);
        if (role == null || role.equals("discushion_server") || role.equals("anon") || role.equals("authenticated")
                || !Boolean.TRUE.equals(jdbc.queryForObject("select has_table_privilege(current_user,'discushion.neighbor_verified_regions','INSERT') and has_table_privilege(current_user,'discushion.institution_credentials','INSERT')", Boolean.class))) {
            throw new IllegalArgumentException("Use the separately configured administrator account");
        }
        // Validate every entry before any provisioning call. Account signup/OTP is a separate prerequisite.
        for (var row : plan.entries()) {
            if (!Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from discushion.users where id=? and registration_completed_at is not null)", Boolean.class, row.memberId()))
                    || !Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from discushion.regions where id=?)", Boolean.class, row.regionId()))) throw new IllegalArgumentException("Member signup or registered region missing");
            if (row.institutionId() != null && !Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from discushion.institutions where id=?)", Boolean.class, row.institutionId()))) throw new IllegalArgumentException("Registered institution missing");
            if (row.kind().equals("unverified") && !Boolean.TRUE.equals(jdbc.queryForObject("select not exists(select 1 from discushion.neighbor_verified_regions where user_id=?) and not exists(select 1 from discushion.institution_credentials where user_id=?)", Boolean.class,row.memberId(),row.memberId()))) throw new IllegalArgumentException("Unverified scenario already has qualifications");
        }
        if (args[0].equals("--check")) { System.out.println("Plan and existing member/region/institution IDs checked; no writes performed."); return; }
        var transactions = new DataSourceTransactionManager(source);
        var neighbors = new NeighborDemoProvisioner(source, transactions, Clock.systemUTC());
        var institutions = new InstitutionDemoProvisioner(source, transactions, Clock.systemUTC());
        for (var row : plan.entries()) {
            if (row.kind().equals("neighbor")) neighbors.add(row.memberId(),row.regionId());
            if (row.kind().equals("institution")) institutions.add(row.memberId(),row.institutionId(),row.regionId(),Instant.parse(row.completedAt()));
        }
        System.out.println("Explicit demo qualifications applied. Requery each account through the actual authenticated API before acceptance.");
    }
}
