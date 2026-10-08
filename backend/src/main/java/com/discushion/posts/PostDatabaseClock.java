package com.discushion.posts;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

final class PostDatabaseClock extends Clock {
    private final PostJdbcStore store;
    PostDatabaseClock(PostJdbcStore store) { this.store = store; }
    @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
    @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }
    @Override public Instant instant() { return store.databaseNow(); }
}
