package com.discushion.region;

/** Existing Region source; no inferred map key, authority or external seed. */
public record Region(long id, String name, String mapFeatureKey) {
    public static final long MAX_ID = 9_007_199_254_740_991L;
}
