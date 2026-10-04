package dev.yasinkilinc.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import tools.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

/** Checks the published API description and saves it to target/openapi.json for the docs site. */
class OpenApiTest extends ApiTestSupport {

	@Test
	void describesEveryEndpointAndTheBearerScheme() throws Exception {
		Response docs = call("GET", "/v3/api-docs", null, null);

		assertThat(docs.status()).isEqualTo(200);
		assertThat(docs.body().get("paths").propertyNames()).contains("/api/auth/token", "/api/rooms",
				"/api/rooms/{id}", "/api/rooms/{id}/schedule", "/api/bookings", "/api/bookings/{id}");
		assertThat(docs.body().at("/components/securitySchemes/bearer/scheme").asString()).isEqualTo("bearer");

		// The test runs on a random port; the saved copy points at the docker compose address instead
		ObjectNode published = (ObjectNode) docs.body();
		published.putArray("servers").addObject().put("url", "http://localhost:8080")
			.put("description", "Local run with docker compose");
		Files.writeString(Path.of("target", "openapi.json"),
				json.writerWithDefaultPrettyPrinter().writeValueAsString(published));
	}

	@Test
	void healthIsPublic() {
		Response health = call("GET", "/actuator/health", null, null);

		assertThat(health.status()).isEqualTo(200);
		assertThat(health.body().get("status").asString()).isEqualTo("UP");
	}

}
