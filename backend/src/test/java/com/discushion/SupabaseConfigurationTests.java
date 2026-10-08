package com.discushion;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.profiles.active=supabase",
        "spring.config.import=",
        "DB_URL=jdbc:postgresql://database.example.invalid:6543/postgres?sslmode=require&prepareThreshold=0",
        "DB_USERNAME=test-user",
        "DB_PASSWORD=local-test-placeholder"
})
class SupabaseConfigurationTests {

    @Autowired
    private HikariDataSource dataSource;

    @Test
    void externalDatabaseSettingsCreateALazyPoolWithoutConnecting() {
        assertThat(dataSource.getJdbcUrl()).endsWith("?sslmode=require&prepareThreshold=0");
        assertThat(dataSource.getUsername()).isEqualTo("test-user");
        assertThat(dataSource.getMaximumPoolSize()).isEqualTo(1);
        assertThat(dataSource.getMinimumIdle()).isZero();
        assertThat(dataSource.isRunning()).isFalse();
    }
}
