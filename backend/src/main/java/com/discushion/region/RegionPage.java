package com.discushion.region;

import java.util.List;

public record RegionPage(List<Region> data, Meta meta) {
    public RegionPage { data = List.copyOf(data); }
    public record Meta(String nextCursor, boolean hasNext) {}
}
