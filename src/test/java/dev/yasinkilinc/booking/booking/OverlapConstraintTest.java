package dev.yasinkilinc.booking.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import dev.yasinkilinc.booking.ApiTestSupport;
import dev.yasinkilinc.booking.auth.Role;
import dev.yasinkilinc.booking.room.Room;

/** Writes straight to the table, bypassing the service, to show the database itself refuses double bookings. */
class OverlapConstraintTest extends ApiTestSupport {

	@Autowired
	private JdbcTemplate jdbc;

	private Room room;

	private final Instant nine = dayAfterTomorrowAt9();

	@BeforeEach
	void setUp() {
		tokenFor("sql-user", Role.USER);
		room = newRoom(4);
	}

	@Test
	void databaseRejectsOverlappingConfirmedBookings() {
		insert(nine, nine.plus(60, ChronoUnit.MINUTES), "CONFIRMED");

		assertThatThrownBy(() -> insert(nine.plus(30, ChronoUnit.MINUTES), nine.plus(90, ChronoUnit.MINUTES),
				"CONFIRMED"))
			.isInstanceOf(DataIntegrityViolationException.class)
			.satisfies(ex -> assertThat(sqlState(ex)).isEqualTo("23P01"))
			.hasMessageContaining("booking_no_overlap");
	}

	@Test
	void cancelledBookingsAndTouchingRangesAreAllowed() {
		insert(nine, nine.plus(60, ChronoUnit.MINUTES), "CONFIRMED");

		assertThatCode(() -> {
			insert(nine, nine.plus(60, ChronoUnit.MINUTES), "CANCELLED");
			insert(nine.plus(60, ChronoUnit.MINUTES), nine.plus(120, ChronoUnit.MINUTES), "CONFIRMED");
		}).doesNotThrowAnyException();
	}

	private void insert(Instant start, Instant end, String status) {
		jdbc.update("""
				insert into booking (room_id, booked_by, title, attendees, starts_at, ends_at, status)
				values (?, 'sql-user', 'direct insert', 1, ?, ?, ?)""", room.getId(), Timestamp.from(start),
				Timestamp.from(end), status);
	}

	private static String sqlState(Throwable ex) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			if (cause instanceof SQLException sql) {
				return sql.getSQLState();
			}
		}
		return null;
	}

}
