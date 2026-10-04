package dev.yasinkilinc.booking.booking;

import java.sql.SQLException;
import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.yasinkilinc.booking.auth.CurrentUser;
import dev.yasinkilinc.booking.common.ApiException;
import dev.yasinkilinc.booking.common.PageResponse;
import dev.yasinkilinc.booking.room.Room;
import dev.yasinkilinc.booking.room.RoomRepository;

@Service
public class BookingService {

	/** PostgreSQL SQLSTATE for exclusion_violation, raised by the booking_no_overlap constraint. */
	private static final String EXCLUSION_VIOLATION = "23P01";

	private final BookingRepository bookings;

	private final RoomRepository rooms;

	private final BookingRules rules;

	private final Clock clock;

	public BookingService(BookingRepository bookings, RoomRepository rooms, BookingRules rules, Clock clock) {
		this.bookings = bookings;
		this.rooms = rooms;
		this.rules = rules;
		this.clock = clock;
	}

	@Transactional
	public BookingResponse create(CurrentUser user, CreateBookingRequest request) {
		// Concurrent inserts that hit the exclusion constraint can deadlock each other in PostgreSQL.
		// Locking the room row queues bookings per room, so the check below always sees committed bookings.
		Room room = rooms.findByIdForUpdate(request.roomId())
			.orElseThrow(() -> ApiException.notFound("Room", request.roomId()));
		rules.check(request, room, clock.instant());
		List<Booking> clashes = bookings.findConfirmedInRange(room.getId(), request.startsAt(), request.endsAt());
		if (!clashes.isEmpty()) {
			Booking clash = clashes.getFirst();
			throw ApiException.conflict("Room '" + room.getName() + "' is already booked at that time",
					Map.of("conflictingBookingId", clash.getId(), "conflictStartsAt", clash.getStartsAt(),
							"conflictEndsAt", clash.getEndsAt()));
		}
		Booking booking = new Booking(room, user.username(), request.title().strip(), request.attendees(),
				request.startsAt(), request.endsAt(), clock.instant());
		try {
			return BookingResponse.of(bookings.saveAndFlush(booking));
		}
		catch (DataIntegrityViolationException ex) {
			// Last line of defence, e.g. a row written by another tool without the room lock
			if (isExclusionViolation(ex)) {
				throw ApiException.conflict("Room '" + room.getName() + "' was just booked by someone else",
						Map.of());
			}
			throw ex;
		}
	}

	@Transactional(readOnly = true)
	public BookingResponse get(CurrentUser user, Long id) {
		return BookingResponse.of(findManageable(user, id));
	}

	@Transactional(readOnly = true)
	public PageResponse<BookingResponse> upcoming(CurrentUser user, Pageable pageable) {
		return PageResponse.of(bookings.findByBookedByAndEndsAtAfter(user.username(), clock.instant(), pageable),
				BookingResponse::of);
	}

	/** Cancelling twice is not an error, so clients can safely retry. */
	@Transactional
	public void cancel(CurrentUser user, Long id) {
		Booking booking = findManageable(user, id);
		if (booking.getStatus() == BookingStatus.CANCELLED) {
			return;
		}
		if (!booking.getStartsAt().isAfter(clock.instant())) {
			throw ApiException.conflict("Booking " + id + " has already started and cannot be cancelled", Map.of());
		}
		booking.cancel();
	}

	private Booking findManageable(CurrentUser user, Long id) {
		Booking booking = bookings.findById(id).orElseThrow(() -> ApiException.notFound("Booking", id));
		if (!user.canManage(booking.getBookedBy())) {
			throw new AccessDeniedException("Booking " + id + " belongs to another user");
		}
		return booking;
	}

	private static boolean isExclusionViolation(Throwable ex) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			if (cause instanceof SQLException sql && EXCLUSION_VIOLATION.equals(sql.getSQLState())) {
				return true;
			}
		}
		return false;
	}

}
