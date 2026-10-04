package dev.yasinkilinc.booking.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.JsonNode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.yasinkilinc.booking.ApiTestSupport;
import dev.yasinkilinc.booking.auth.Role;
import dev.yasinkilinc.booking.room.Room;

class BookingApiTest extends ApiTestSupport {

	private final Instant nine = dayAfterTomorrowAt9();

	private final Instant ten = nine.plus(1, ChronoUnit.HOURS);

	private final Instant eleven = nine.plus(2, ChronoUnit.HOURS);

	private String alice;

	private String bob;

	private Room room;

	@BeforeEach
	void setUp() {
		alice = tokenFor("alice", Role.USER);
		bob = tokenFor("bob", Role.USER);
		room = newRoom(8);
	}

	@Test
	void createsABooking() {
		Response response = call("POST", "/api/bookings", alice, booking(room, nine, ten));

		assertThat(response.status()).isEqualTo(201);
		JsonNode body = response.body();
		assertThat(response.raw().headers().firstValue("Location")).hasValue("/api/bookings/" + body.get("id").asLong());
		assertThat(body.get("bookedBy").asString()).isEqualTo("alice");
		assertThat(body.get("roomName").asString()).isEqualTo(room.getName());
		assertThat(body.get("status").asString()).isEqualTo("CONFIRMED");
		assertThat(Instant.parse(body.get("startsAt").asString())).isEqualTo(nine);
	}

	@Test
	void overlappingBookingIs409AndNamesTheConflict() {
		long first = call("POST", "/api/bookings", alice, booking(room, nine, eleven)).body().get("id").asLong();

		Response clash = call("POST", "/api/bookings", bob, booking(room, ten, ten.plus(30, ChronoUnit.MINUTES)));

		assertThat(clash.status()).isEqualTo(409);
		assertThat(clash.body().get("conflictingBookingId").asLong()).isEqualTo(first);
		assertThat(clash.body().get("detail").asString()).contains("already booked");
	}

	@Test
	void backToBackBookingsAreAllowed() {
		assertThat(call("POST", "/api/bookings", alice, booking(room, nine, ten)).status()).isEqualTo(201);
		assertThat(call("POST", "/api/bookings", bob, booking(room, ten, eleven)).status()).isEqualTo(201);
	}

	@Test
	void sameTimeInAnotherRoomIsFine() {
		Room other = newRoom(8);
		assertThat(call("POST", "/api/bookings", alice, booking(room, nine, ten)).status()).isEqualTo(201);
		assertThat(call("POST", "/api/bookings", alice, booking(other, nine, ten)).status()).isEqualTo(201);
	}

	@Test
	void cancellingFreesTheSlot() {
		long id = call("POST", "/api/bookings", alice, booking(room, nine, ten)).body().get("id").asLong();

		assertThat(call("DELETE", "/api/bookings/" + id, alice, null).status()).isEqualTo(204);
		assertThat(call("GET", "/api/bookings/" + id, alice, null).body().get("status").asString())
			.isEqualTo("CANCELLED");
		assertThat(call("POST", "/api/bookings", bob, booking(room, nine, ten)).status()).isEqualTo(201);
	}

	@Test
	void cancellingTwiceIsNotAnError() {
		long id = call("POST", "/api/bookings", alice, booking(room, nine, ten)).body().get("id").asLong();

		assertThat(call("DELETE", "/api/bookings/" + id, alice, null).status()).isEqualTo(204);
		assertThat(call("DELETE", "/api/bookings/" + id, alice, null).status()).isEqualTo(204);
	}

	@Test
	void usersCannotSeeOrCancelOthersBookingsButAdminsCan() {
		long id = call("POST", "/api/bookings", alice, booking(room, nine, ten)).body().get("id").asLong();
		String admin = tokenFor("admin", Role.ADMIN);

		assertThat(call("GET", "/api/bookings/" + id, bob, null).status()).isEqualTo(403);
		assertThat(call("DELETE", "/api/bookings/" + id, bob, null).status()).isEqualTo(403);
		assertThat(call("GET", "/api/bookings/" + id, admin, null).status()).isEqualTo(200);
		assertThat(call("DELETE", "/api/bookings/" + id, admin, null).status()).isEqualTo(204);
	}

	@Test
	void missingFieldsAreListed() {
		Response response = call("POST", "/api/bookings", alice, Map.of("title", " "));

		assertThat(response.status()).isEqualTo(400);
		List<String> fields = new ArrayList<>();
		response.body().get("errors").forEach(error -> fields.add(error.get("field").asString()));
		assertThat(fields).containsExactlyInAnyOrder("roomId", "title", "attendees", "startsAt", "endsAt");
	}

	@Test
	void businessRulesAreChecked() {
		Map<String, Object> tooMany = Map.of("roomId", room.getId(), "title", "All hands", "attendees", 9, "startsAt",
				nine.toString(), "endsAt", ten.toString());

		Response response = call("POST", "/api/bookings", alice, tooMany);

		assertThat(response.status()).isEqualTo(400);
		assertThat(response.body().get("detail").asString()).contains("holds 8 people, not 9");
	}

	@Test
	void unknownRoomIs404() {
		Map<String, Object> request = Map.of("roomId", 999_999, "title", "Ghost", "attendees", 1, "startsAt",
				nine.toString(), "endsAt", ten.toString());

		assertThat(call("POST", "/api/bookings", alice, request).status()).isEqualTo(404);
	}

	@Test
	void listsOnlyMyUpcomingBookingsPagedAndSorted() {
		String carol = tokenFor("carol-" + room.getId(), Role.USER);
		for (int i = 2; i >= 0; i--) {
			Instant start = nine.plus(i, ChronoUnit.HOURS);
			call("POST", "/api/bookings", carol, booking(room, start, start.plus(30, ChronoUnit.MINUTES)));
		}
		call("POST", "/api/bookings", bob, booking(room, nine.plus(4, ChronoUnit.HOURS), nine.plus(5, ChronoUnit.HOURS)));

		Response firstPage = call("GET", "/api/bookings?size=2", carol, null);

		assertThat(firstPage.status()).isEqualTo(200);
		assertThat(firstPage.body().get("totalElements").asInt()).isEqualTo(3);
		assertThat(firstPage.body().get("totalPages").asInt()).isEqualTo(2);
		JsonNode content = firstPage.body().get("content");
		assertThat(Instant.parse(content.get(0).get("startsAt").asString())).isEqualTo(nine);
		assertThat(Instant.parse(content.get(1).get("startsAt").asString())).isEqualTo(ten);
	}

	@Test
	void unknownSortFieldIs400() {
		assertThat(call("GET", "/api/bookings?sort=password", alice, null).status()).isEqualTo(400);
	}

	@Test
	void roomScheduleShowsConfirmedBookingsInOrder() {
		long cancelled = call("POST", "/api/bookings", alice, booking(room, nine, ten)).body().get("id").asLong();
		call("DELETE", "/api/bookings/" + cancelled, alice, null);
		call("POST", "/api/bookings", bob, booking(room, eleven, eleven.plus(1, ChronoUnit.HOURS)));
		call("POST", "/api/bookings", alice, booking(room, ten, eleven));

		Response schedule = call("GET", "/api/rooms/" + room.getId() + "/schedule?from=" + nine + "&to="
				+ nine.plus(1, ChronoUnit.DAYS), bob, null);

		assertThat(schedule.status()).isEqualTo(200);
		assertThat(schedule.body()).hasSize(2);
		assertThat(schedule.body().get(0).get("bookedBy").asString()).isEqualTo("alice");
		assertThat(schedule.body().get(1).get("bookedBy").asString()).isEqualTo("bob");
	}

}
