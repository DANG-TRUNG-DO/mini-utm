package com.portfolio.mini_utm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class MiniUtmApplicationTests extends PostgresIntegrationTest {

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
				  AND table_name IN ('drones', 'missions', 'mission_waypoints', 'geofences', 'telemetry', 'alerts')
				ORDER BY table_name
				""", String.class);

		assertThat(successfulMigrations).isEqualTo(2);
		assertThat(tables).containsExactly(
				"alerts", "drones", "geofences", "mission_waypoints", "missions", "telemetry");
	}

	@Test
	void databaseConstraintsMatchMvpBusinessLifecycles() {
		Map<String, String> constraints = jdbcTemplate.query("""
				SELECT constraint_name, check_clause
				FROM information_schema.check_constraints
				WHERE constraint_name IN (
				  'ck_drones_status',
				  'ck_missions_status',
				  'ck_alerts_type',
				  'ck_alerts_severity',
				  'ck_alerts_status'
				)
				""", resultSet -> {
			Map<String, String> result = new java.util.HashMap<>();
			while (resultSet.next()) {
				result.put(resultSet.getString("constraint_name"), resultSet.getString("check_clause"));
			}
			return result;
		});

		assertThat(constraints.get("ck_drones_status"))
				.contains("ACTIVE", "INACTIVE", "MAINTENANCE")
				.doesNotContain("OFFLINE", "IDLE", "IN_MISSION", "LOST");
		assertThat(constraints.get("ck_missions_status"))
				.contains("PLANNED", "APPROVED", "ACTIVE", "COMPLETED", "CANCELLED")
				.doesNotContain("PAUSED", "FAILED");
		assertThat(constraints.get("ck_alerts_type"))
				.contains("GEOFENCE_VIOLATION", "TELEMETRY_LOSS", "LOW_BATTERY", "ROUTE_DEVIATION");
		assertThat(constraints.get("ck_alerts_severity"))
				.contains("INFO", "WARNING", "CRITICAL");
		assertThat(constraints.get("ck_alerts_status"))
				.contains("OPEN", "ACKNOWLEDGED", "RESOLVED");
	}

	@Test
	void alertDeduplicationMigrationCreatesPartialUniqueIndex() {
		String indexDefinition = jdbcTemplate.queryForObject("""
				SELECT indexdef
				FROM pg_indexes
				WHERE schemaname = 'public'
				  AND indexname = 'uk_alerts_active_dedup'
				""", String.class);

		assertThat(indexDefinition)
				.containsIgnoringCase("UNIQUE")
				.contains("drone_id", "type", "dedup_key")
				.contains("status", "RESOLVED");
	}
}
