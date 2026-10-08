package com.discushion.photos;

import java.time.*;

/** One DB time authority for upload admission and relay file lifecycle across server instances. */
final class PhotoDatabaseClock extends Clock {
    private final JdbcPhotoStore store;
    private final ZoneId zone;
    PhotoDatabaseClock(JdbcPhotoStore store) {this(store,ZoneOffset.UTC);}
    private PhotoDatabaseClock(JdbcPhotoStore store,ZoneId zone){this.store=store;this.zone=zone;}
    public Instant instant(){return store.databaseNow();}
    public ZoneId getZone(){return zone;}
    public Clock withZone(ZoneId zone){return new PhotoDatabaseClock(store,zone);}
}
