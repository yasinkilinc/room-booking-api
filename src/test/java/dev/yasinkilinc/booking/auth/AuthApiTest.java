package dev.yasinkilinc.booking.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.yasinkilinc.booking.ApiTestSupport;

class AuthApiTest extends ApiTestSupport {

	@Test
	void loginReturnsABearerToken() {
		tokenFor("auth-alice", Role.USER);
		Response login = call("POST", "/api/auth/token", null, Map.of("username", "auth-alice", "password", PASSWORD));

		assertThat(login.status()).isEqualTo(200);
		assertThat(login.body().get("tokenType").asString()).isEqualTo("Bearer");
		assertThat(login.body().get("expiresIn").asLong()).isEqualTo(3600);
		assertThat(login.body().get("accessToken").asString()).hasSizeGreaterThan(100);
	}

	@Test
	void wrongPasswordAndUnknownUserGetTheSameAnswer() {
		tokenFor("auth-bob", Role.USER);
		Response wrongPassword = call("POST", "/api/auth/token", null,
				Map.of("username", "auth-bob", "password", "nope"));
		Response unknownUser = call("POST", "/api/auth/token", null,
				Map.of("username", "nobody", "password", "nope"));

		assertThat(wrongPassword.status()).isEqualTo(401);
		assertThat(unknownUser.status()).isEqualTo(401);
		assertThat(wrongPassword.body().get("detail")).isEqualTo(unknownUser.body().get("detail"));
	}

	@Test
	void requestWithoutTokenIs401WithProblemBody() {
		Response response = call("GET", "/api/rooms", null, null);

		assertThat(response.status()).isEqualTo(401);
		assertThat(response.raw().headers().firstValue("WWW-Authenticate")).hasValueSatisfying(
				value -> assertThat(value).startsWith("Bearer"));
		assertThat(response.body().get("status").asInt()).isEqualTo(401);
	}

	@Test
	void tamperedTokenIsRejected() {
		String token = tokenFor("auth-carol", Role.USER);
		String tampered = token.substring(0, token.length() - 4) + "AAAA";

		assertThat(call("GET", "/api/rooms", tampered, null).status()).isEqualTo(401);
	}

	@Test
	void onlyAdminsCanCreateRooms() {
		String user = tokenFor("auth-dave", Role.USER);
		String admin = tokenFor("auth-admin", Role.ADMIN);
		Map<String, Object> room = Map.of("name", "Auth test room", "capacity", 6);

		Response denied = call("POST", "/api/rooms", user, room);
		assertThat(denied.status()).isEqualTo(403);
		assertThat(denied.body().get("status").asInt()).isEqualTo(403);

		Response created = call("POST", "/api/rooms", admin, room);
		assertThat(created.status()).isEqualTo(201);
		assertThat(created.raw().headers().firstValue("Location"))
			.hasValue("/api/rooms/" + created.body().get("id").asLong());
	}

	@Test
	void roomNamesAreUniqueIgnoringCase() {
		String admin = tokenFor("auth-admin", Role.ADMIN);
		call("POST", "/api/rooms", admin, Map.of("name", "Library", "capacity", 6));

		Response duplicate = call("POST", "/api/rooms", admin, Map.of("name", "  LIBRARY ", "capacity", 3));

		assertThat(duplicate.status()).isEqualTo(409);
	}

}
