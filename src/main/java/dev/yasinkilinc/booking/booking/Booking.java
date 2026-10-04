package dev.yasinkilinc.booking.booking;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;

import dev.yasinkilinc.booking.room.Room;

@Entity
public class Booking {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id")
	private Room room;

	@Column(name = "booked_by", nullable = false)
	private String bookedBy;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private int attendees;

	@Column(name = "starts_at", nullable = false)
	private Instant startsAt;

	@Column(name = "ends_at", nullable = false)
	private Instant endsAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private BookingStatus status = BookingStatus.CONFIRMED;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Version
	private long version;

	protected Booking() {
	}

	public Booking(Room room, String bookedBy, String title, int attendees, Instant startsAt, Instant endsAt,
			Instant createdAt) {
		this.room = room;
		this.bookedBy = bookedBy;
		this.title = title;
		this.attendees = attendees;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.createdAt = createdAt;
	}

	public void cancel() {
		this.status = BookingStatus.CANCELLED;
	}

	public Long getId() {
		return id;
	}

	public Room getRoom() {
		return room;
	}

	public String getBookedBy() {
		return bookedBy;
	}

	public String getTitle() {
		return title;
	}

	public int getAttendees() {
		return attendees;
	}

	public Instant getStartsAt() {
		return startsAt;
	}

	public Instant getEndsAt() {
		return endsAt;
	}

	public BookingStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
