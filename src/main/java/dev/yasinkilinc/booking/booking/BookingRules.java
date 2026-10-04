package dev.yasinkilinc.booking.booking;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

import dev.yasinkilinc.booking.common.ApiException;
import dev.yasinkilinc.booking.room.Room;

/** Checks a booking request against the room and the configured limits. Overlaps are checked separately. */
@Component
public class BookingRules {

	private final BookingRulesProperties limits;

	public BookingRules(BookingRulesProperties limits) {
		this.limits = limits;
	}

	public void check(CreateBookingRequest request, Room room, Instant now) {
		Instant start = request.startsAt();
		Instant end = request.endsAt();
		if (!room.isActive()) {
			throw ApiException.badRequest("Room '" + room.getName() + "' is closed for bookings");
		}
		if (!end.isAfter(start)) {
			throw ApiException.badRequest("endsAt must be after startsAt");
		}
		if (start.isBefore(now)) {
			throw ApiException.badRequest("startsAt is in the past");
		}
		if (start.isAfter(now.plus(Duration.ofDays(limits.maxDaysAhead())))) {
			throw ApiException.badRequest("Bookings can be made at most " + limits.maxDaysAhead() + " days ahead");
		}
		if (Duration.between(start, end).compareTo(limits.maxDuration()) > 0) {
			throw ApiException.badRequest("A booking can last at most " + limits.maxDuration().toHours() + " hours");
		}
		if (!onSlotGrid(start) || !onSlotGrid(end)) {
			throw ApiException.badRequest("Start and end must be on a " + limits.slotMinutes() + "-minute boundary");
		}
		if (request.attendees() > room.getCapacity()) {
			throw ApiException.badRequest(
					"Room '" + room.getName() + "' holds " + room.getCapacity() + " people, not " + request.attendees());
		}
	}

	private boolean onSlotGrid(Instant time) {
		long slotSeconds = limits.slotMinutes() * 60L;
		return time.getEpochSecond() % slotSeconds == 0 && time.getNano() == 0;
	}

}
