package dev.yasinkilinc.booking.booking;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import dev.yasinkilinc.booking.common.ApiException;
import dev.yasinkilinc.booking.room.Room;

class BookingRulesTest {

	private static final Instant NOW = Instant.parse("2026-10-05T08:07:13Z");

	private static final Instant TOMORROW_9 = Instant.parse("2026-10-06T09:00:00Z");

	private final BookingRules rules = new BookingRules(new BookingRulesProperties(15, Duration.ofHours(8), 90));

	private final Room room = new Room("Galata", 4);

	@Test
	void acceptsAValidBooking() {
		assertThatCode(() -> rules.check(request(TOMORROW_9, TOMORROW_9.plusSeconds(3600), 4), room, NOW))
			.doesNotThrowAnyException();
	}

	@Test
	void rejectsEndBeforeStart() {
		assertRejected(request(TOMORROW_9, TOMORROW_9.minusSeconds(900), 2), "endsAt must be after startsAt");
	}

	@Test
	void rejectsZeroLength() {
		assertRejected(request(TOMORROW_9, TOMORROW_9, 2), "endsAt must be after startsAt");
	}

	@Test
	void rejectsStartInThePast() {
		Instant earlierToday = Instant.parse("2026-10-05T08:00:00Z");
		assertRejected(request(earlierToday, earlierToday.plusSeconds(900), 2), "in the past");
	}

	@Test
	void rejectsBookingTooFarAhead() {
		Instant farAway = Instant.parse("2027-01-10T09:00:00Z");
		assertRejected(request(farAway, farAway.plusSeconds(900), 2), "at most 90 days ahead");
	}

	@Test
	void acceptsExactlyMaxDurationButNotMore() {
		assertThatCode(() -> rules.check(request(TOMORROW_9, TOMORROW_9.plus(Duration.ofHours(8)), 2), room, NOW))
			.doesNotThrowAnyException();
		assertRejected(request(TOMORROW_9, TOMORROW_9.plus(Duration.ofHours(8).plusMinutes(15)), 2), "at most 8 hours");
	}

	@Test
	void rejectsTimesOffTheSlotGrid() {
		assertRejected(request(TOMORROW_9.plusSeconds(600), TOMORROW_9.plusSeconds(3600), 2), "15-minute boundary");
		assertRejected(request(TOMORROW_9, TOMORROW_9.plusSeconds(3601), 2), "15-minute boundary");
	}

	@Test
	void rejectsMorePeopleThanTheRoomHolds() {
		assertRejected(request(TOMORROW_9, TOMORROW_9.plusSeconds(3600), 5), "holds 4 people, not 5");
	}

	private void assertRejected(CreateBookingRequest request, String message) {
		assertThatThrownBy(() -> rules.check(request, room, NOW)).isInstanceOf(ApiException.class)
			.hasMessageContaining(message);
	}

	private static CreateBookingRequest request(Instant start, Instant end, int attendees) {
		return new CreateBookingRequest(1L, "Planning", attendees, start, end);
	}

}
