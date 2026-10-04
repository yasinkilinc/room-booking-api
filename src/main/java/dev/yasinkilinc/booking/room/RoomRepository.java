package dev.yasinkilinc.booking.room;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, Long> {

	List<Room> findAllByOrderByName();

	/** Locks the room row so bookings for the same room are made one at a time. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from Room r where r.id = :id")
	Optional<Room> findByIdForUpdate(@Param("id") Long id);

	boolean existsByNameIgnoreCase(String name);

}
