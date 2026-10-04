package dev.yasinkilinc.booking.booking;

import java.time.Instant;

public record BookingResponse(Long id, Long roomId, String roomName, String bookedBy, String title, int attendees,
		Instant startsAt, Instant endsAt, BookingStatus status) {

	public static BookingResponse of(Booking booking) {
		return new BookingResponse(booking.getId(), booking.getRoom().getId(), booking.getRoom().getName(),
				booking.getBookedBy(), booking.getTitle(), booking.getAttendees(), booking.getStartsAt(),
				booking.getEndsAt(), booking.getStatus());
	}

}
