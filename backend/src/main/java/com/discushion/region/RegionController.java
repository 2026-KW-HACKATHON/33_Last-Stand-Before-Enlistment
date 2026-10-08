package com.discushion.region;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class RegionController {
    private final ObjectProvider<JdbcRegionCatalog> catalogs;

    public RegionController(ObjectProvider<JdbcRegionCatalog> catalogs) { this.catalogs = catalogs; }

    @GetMapping("/api/v1/regions")
    public RegionPage list(@RequestParam MultiValueMap<String, String> params) {
        var query = RegionQuery.parse(params);
        var catalog = catalogs.getIfAvailable();
        if (catalog == null) throw new IllegalStateException("Region DataSource is not configured");
        return catalog.list(query);
    }
}
