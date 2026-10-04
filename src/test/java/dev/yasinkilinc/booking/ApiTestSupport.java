package dev.yasinkilinc.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

import dev.yasinkilinc.booking.auth.Role;
import dev.yasinkilinc.booking.auth.UserAccount;
import dev.yasinkilinc.booking.auth.UserAccountRepository;
import dev.yasinkilinc.booking.room.Room;
import dev.yasinkilinc.booking.room.RoomRepository;

/**
 * Starts the app on a random port against PostgreSQL in Docker and calls it over real HTTP.
 * Each test creates its own rooms, so tests do not depend on each other's bookings.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "booking.jwt.secret=test-only-secret-that-is-at-least-32-bytes")
public abstract class ApiTestSupport {

	protected static final String PASSWORD = "correct horse battery staple";

	private static final AtomicInteger ROOM_SEQUENCE = new AtomicInteger();

	private static final HttpClient HTTP = HttpClient.newHttpClient();

	@LocalServerPort
	private int port;

	@Autowired
	protected JsonMapper json;

	@Autowired
	private UserAccountRepository users;

	@Autowired
	private RoomRepository rooms;

	@Autowired
	private PasswordEncoder passwordEncoder;

	protected record Response(int status, JsonNode body, HttpResponse<String> raw) {
	}

	/** Creates the user on first use and returns a fresh bearer token. */
	protected String tokenFor(String username, Role role) {
		synchronized (ApiTestSupport.class) {
			if (!users.existsByUsername(username)) {
				users.save(new UserAccount(username, passwordEncoder.encode(PASSWORD), role));
			}
		}
		Response login = call("POST", "/api/auth/token", null, Map.of("username", username, "password", PASSWORD));
		assertThat(login.status()).isEqualTo(200);
		return login.body().get("accessToken").asString();
	}

	protected Room newRoom(int capacity) {
		return rooms.save(new Room("Room " + ROOM_SEQUENCE.incrementAndGet() + " " + UUID.randomUUID(), capacity));
	}

	/** 09:00 UTC, two days from now: always in the future and on the 15-minute grid. */
	protected static Instant dayAfterTomorrowAt9() {
		return Instant.now().truncatedTo(ChronoUnit.DAYS).plus(2, ChronoUnit.DAYS).plus(9, ChronoUnit.HOURS);
	}

	protected static Map<String, Object> booking(Room room, Instant start, Instant end) {
		return Map.of("roomId", room.getId(), "title", "Sprint planning", "attendees", 2, "startsAt",
				start.toString(), "endsAt", end.toString());
	}

	protected Response call(String method, String path, String token, Object body) {
		try {
			HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.header("Content-Type", "application/json")
				.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
						: HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
			if (token != null) {
				request.header("Authorization", "Bearer " + token);
			}
			HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
			JsonNode parsed = response.body().isBlank() ? null : json.readTree(response.body());
			return new Response(response.statusCode(), parsed, response);
		}
		catch (Exception ex) {
			throw new IllegalStateException(method + " " + path + " failed", ex);
		}
	}

}
