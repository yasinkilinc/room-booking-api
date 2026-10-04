package dev.yasinkilinc.booking.booking;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

	/** Confirmed bookings of the room that overlap [from, to). Touching ranges do not overlap. */
	@EntityGraph(attributePaths = "room")
	@Query("""
			select b from Booking b
			where b.room.id = :roomId and b.status = 'CONFIRMED'
			  and b.startsAt < :to and b.endsAt > :from
			order by b.startsAt""")
	List<Booking> findConfirmedInRange(@Param("roomId") Long roomId, @Param("from") Instant from,
			@Param("to") Instant to);

	@EntityGraph(attributePaths = "room")
	Page<Booking> findByBookedByAndEndsAtAfter(String bookedBy, Instant after, Pageable pageable);

}
