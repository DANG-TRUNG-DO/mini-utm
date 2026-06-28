package com.portfolio.mini_utm;

import static org.hamcrest.Matchers.hasSize;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.mini_utm.drone.repository.DroneRepository;

class DroneApiIntegrationTests extends PostgresIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private DroneRepository droneRepository;

	@BeforeEach
	void cleanDatabase() {
		droneRepository.deleteAll();
	}

	@Test
	void registerUpdateListAndChangeDroneStatus() throws Exception {
		String body = mockMvc.perform(post("/api/v1/drones")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "serialNumber": " uav-001 ",
							  "name": "Survey One",
							  "model": "Quad-X"
							}
							"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/v1/drones/.+")))
				.andExpect(jsonPath("$.serialNumber").value("UAV-001"))
				.andExpect(jsonPath("$.status").value("INACTIVE"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		UUID droneId = UUID.fromString(objectMapper.readTree(body).get("id").asText());

		mockMvc.perform(patch("/api/v1/drones/{id}", droneId)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"name":"Survey One Updated","model":"Quad-X2"}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Survey One Updated"))
				.andExpect(jsonPath("$.model").value("Quad-X2"));

		mockMvc.perform(patch("/api/v1/drones/{id}/status", droneId)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"status":"ACTIVE"}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACTIVE"));

		mockMvc.perform(get("/api/v1/drones"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(droneId.toString()));

		mockMvc.perform(get("/api/v1/drones/{id}", droneId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Survey One Updated"));
	}

	@Test
	void rejectDuplicateSerialAndInvalidStatusTransition() throws Exception {
		UUID droneId = register("uav-duplicate");

		mockMvc.perform(post("/api/v1/drones")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"serialNumber":"UAV-DUPLICATE","name":"Duplicate"}
							"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Drone serial number already exists: UAV-DUPLICATE"));

		changeStatus(droneId, "MAINTENANCE", 200);
		changeStatus(droneId, "ACTIVE", 409);
	}

	@Test
	void validateRequestsAndReturnNotFound() throws Exception {
		mockMvc.perform(post("/api/v1/drones")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"serialNumber":"   ","name":""}
							"""))
				.andExpect(status().isBadRequest());

		mockMvc.perform(get("/api/v1/drones/{id}", UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Drone not found"));
	}

	private UUID register(String serialNumber) throws Exception {
		String response = mockMvc.perform(post("/api/v1/drones")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(new RegisterPayload(serialNumber, "Test drone"))))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		JsonNode json = objectMapper.readTree(response);
		return UUID.fromString(json.get("id").asText());
	}

	private void changeStatus(UUID id, String statusValue, int expectedStatus) throws Exception {
		mockMvc.perform(patch("/api/v1/drones/{id}/status", id)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"status\":\"" + statusValue + "\"}"))
				.andExpect(status().is(expectedStatus));
	}

	private record RegisterPayload(String serialNumber, String name) {
	}
}
