package com.discushion.region;

import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
public class RegionConfiguration {
    @Bean
    @Profile("!local")
    JdbcRegionCatalog jdbcRegionCatalog(DataSource source) { return new JdbcRegionCatalog(source); }
}
