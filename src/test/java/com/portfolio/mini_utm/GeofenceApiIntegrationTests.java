package com.portfolio.mini_utm;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.mini_utm.geofence.repository.GeofenceRepository;

class GeofenceApiIntegrationTests extends PostgresIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private GeofenceRepository geofenceRepository;

	@BeforeEach
	void cleanDatabase() {
		geofenceRepository.deleteAll();
	}

	@Test
	void createListAndGetGeofenceUsingGeoJsonPolygon() throws Exception {
		String responseBody = mockMvc.perform(post("/api/v1/geofences")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validRequest("SGN-RESTRICTED")))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/v1/geofences/.+")))
				.andExpect(jsonPath("$.name").value("SGN-RESTRICTED"))
				.andExpect(jsonPath("$.boundary.type").value("Polygon"))
				.andExpect(jsonPath("$.boundary.coordinates[0]", hasSize(5)))
				.andExpect(jsonPath("$.boundary.coordinates[0][0][0]").value(106.69))
				.andExpect(jsonPath("$.boundary.coordinates[0][0][1]").value(10.77))
				.andExpect(jsonPath("$.minAltitudeM").value(20.0))
				.andExpect(jsonPath("$.maxAltitudeM").value(120.0))
				.andExpect(jsonPath("$.active").value(true))
				.andReturn()
				.getResponse()
				.getContentAsString();

		UUID id = UUID.fromString(objectMapper.readTree(responseBody).get("id").asText());

		mockMvc.perform(get("/api/v1/geofences"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(id.toString()));

		mockMvc.perform(get("/api/v1/geofences/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("SGN-RESTRICTED"));
	}

	@Test
	void rejectDuplicateNameIgnoringCase() throws Exception {
		mockMvc.perform(post("/api/v1/geofences")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validRequest("SGN-RESTRICTED")))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/geofences")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validRequest("sgn-restricted")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Geofence conflict"));
	}

	@Test
	void rejectOpenPolygonInvalidAltitudeAndInvalidValidityPeriod() throws Exception {
		mockMvc.perform(post("/api/v1/geofences")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name":"OPEN-POLYGON",
							  "boundary":{"type":"Polygon","coordinates":[[
							    [106.69,10.77],[106.70,10.77],[106.70,10.78],[106.69,10.78]
							  ]]},
							  "active":true
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Each polygon ring must be closed"));

		mockMvc.perform(post("/api/v1/geofences")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validRequest("INVALID-ALTITUDE").replace(
							"\"minAltitudeM\":20.00", "\"minAltitudeM\":200.00")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Minimum altitude must not exceed maximum altitude"));

		mockMvc.perform(post("/api/v1/geofences")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validRequest("INVALID-TIME").replace(
							"2026-06-29T00:00:00Z", "2026-06-27T00:00:00Z")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("validFrom must be before validUntil"));
	}

	@Test
	void returnNotFoundForUnknownGeofence() throws Exception {
		UUID unknownId = UUID.randomUUID();
		mockMvc.perform(get("/api/v1/geofences/{id}", unknownId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Geofence not found"));

		mockMvc.perform(patch("/api/v1/geofences/{id}", unknownId)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"active\":false}"))
				.andExpect(status().isNotFound());

		mockMvc.perform(delete("/api/v1/geofences/{id}", unknownId))
				.andExpect(status().isNotFound());
	}

	@Test
	void partiallyUpdateGeofenceAndPreserveUnspecifiedFields() throws Exception {
		UUID id = createGeofence("SGN-UPDATE");

		mockMvc.perform(patch("/api/v1/geofences/{id}", id)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name":"SGN-UPDATED",
							  "description":"Updated restricted area",
							  "active":false,
							  "maxAltitudeM":150.00,
							  "boundary":{"type":"Polygon","coordinates":[[
							    [106.68,10.76],
							    [106.71,10.76],
							    [106.71,10.79],
							    [106.68,10.79],
							    [106.68,10.76]
							  ]]}
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("SGN-UPDATED"))
				.andExpect(jsonPath("$.description").value("Updated restricted area"))
				.andExpect(jsonPath("$.active").value(false))
				.andExpect(jsonPath("$.minAltitudeM").value(20.0))
				.andExpect(jsonPath("$.maxAltitudeM").value(150.0))
				.andExpect(jsonPath("$.boundary.coordinates[0][0][0]").value(106.68))
				.andExpect(jsonPath("$.validFrom").value("2026-06-28T00:00:00Z"));

		mockMvc.perform(patch("/api/v1/geofences/{id}", id)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"active\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("SGN-UPDATED"))
				.andExpect(jsonPath("$.active").value(true))
				.andExpect(jsonPath("$.maxAltitudeM").value(150.0));
	}

	@Test
	void rejectDuplicateNameAndInvalidMergedStateDuringUpdate() throws Exception {
		UUID firstId = createGeofence("ZONE-ONE");
		createGeofence("ZONE-TWO");

		mockMvc.perform(patch("/api/v1/geofences/{id}", firstId)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"zone-two\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Geofence conflict"));

		mockMvc.perform(patch("/api/v1/geofences/{id}", firstId)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"minAltitudeM\":200.00}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Minimum altitude must not exceed maximum altitude"));

		mockMvc.perform(patch("/api/v1/geofences/{id}", firstId)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"validUntil\":\"2026-06-27T00:00:00Z\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("validFrom must be before validUntil"));
	}

	@Test
	void deleteGeofenceAndReturnNoContent() throws Exception {
		UUID id = createGeofence("ZONE-DELETE");

		mockMvc.perform(delete("/api/v1/geofences/{id}", id))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/geofences/{id}", id))
				.andExpect(status().isNotFound());
	}

	private UUID createGeofence(String name) throws Exception {
		String response = mockMvc.perform(post("/api/v1/geofences")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validRequest(name)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return UUID.fromString(objectMapper.readTree(response).get("id").asText());
	}

	private String validRequest(String name) {
		return """
				{
				  "name":"%s",
				  "description":"Demo restricted area",
				  "boundary":{"type":"Polygon","coordinates":[[
				    [106.69,10.77],
				    [106.70,10.77],
				    [106.70,10.78],
				    [106.69,10.78],
				    [106.69,10.77]
				  ]]},
				  "minAltitudeM":20.00,
				  "maxAltitudeM":120.00,
				  "active":true,
				  "validFrom":"2026-06-28T00:00:00Z",
				  "validUntil":"2026-06-29T00:00:00Z"
				}
				""".formatted(name);
	}
}
