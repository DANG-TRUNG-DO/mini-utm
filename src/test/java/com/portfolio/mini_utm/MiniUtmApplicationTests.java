package com.portfolio.mini_utm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class MiniUtmApplicationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
			DockerImageName.parse("postgis/postgis:17-3.5")
					.asCompatibleSubstituteFor("postgres"))
					.withEnv("TZ", "UTC")
					.withEnv("PGTZ", "UTC");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads() {
	}

	@Test
	void flywayMigrationsCreateInitialSchemaOnEmptyDatabase() {
		Integer successfulMigrations = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM flyway_schema_history WHERE success", Integer.class);
		List<String> tables = jdbcTemplate.queryForList("""
				SELECT table_name
				FROM information_schema.tables
				WHERE table_schema = 'public'
				  AND table_name IN ('drones', 'missions', 'geofences', 'telemetry', 'alerts')
				ORDER BY table_name
				""", String.class);

		assertThat(successfulMigrations).isEqualTo(1);
		assertThat(tables).containsExactly("alerts", "drones", "geofences", "missions", "telemetry");
	}
}
