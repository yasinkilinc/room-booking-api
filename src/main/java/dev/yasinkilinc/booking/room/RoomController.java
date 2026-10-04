package dev.yasinkilinc.booking.room;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.yasinkilinc.booking.booking.BookingRepository;
import dev.yasinkilinc.booking.booking.BookingResponse;
import dev.yasinkilinc.booking.common.ApiException;

@Tag(name = "Rooms")
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

	private static final Duration MAX_SCHEDULE_RANGE = Duration.ofDays(31);

	private final RoomRepository rooms;

	private final BookingRepository bookings;

	public RoomController(RoomRepository rooms, BookingRepository bookings) {
		this.rooms = rooms;
		this.bookings = bookings;
	}

	public record CreateRoomRequest(@NotBlank @Size(max = 80) String name, @NotNull @Min(1) @Max(500) Integer capacity) {
	}

	public record RoomResponse(Long id, String name, int capacity, boolean active) {

		static RoomResponse of(Room room) {
			return new RoomResponse(room.getId(), room.getName(), room.getCapacity(), room.isActive());
		}

	}

	@Operation(summary = "List all rooms")
	@GetMapping
	public List<RoomResponse> list() {
		return rooms.findAllByOrderByName().stream().map(RoomResponse::of).toList();
	}

	@Operation(summary = "Get one room")
	@GetMapping("/{id}")
	public RoomResponse get(@PathVariable Long id) {
		return RoomResponse.of(rooms.findById(id).orElseThrow(() -> ApiException.notFound("Room", id)));
	}

	@Operation(summary = "Create a room (admin only)")
	@PostMapping
	@Transactional
	public ResponseEntity<RoomResponse> create(@Valid @RequestBody CreateRoomRequest request) {
		String name = request.name().strip();
		if (rooms.existsByNameIgnoreCase(name)) {
			throw ApiException.conflict("A room named '" + name + "' already exists", Map.of());
		}
		Room room = rooms.save(new Room(name, request.capacity()));
		return ResponseEntity.created(URI.create("/api/rooms/" + room.getId())).body(RoomResponse.of(room));
	}

	@Operation(summary = "Confirmed bookings of a room in a time range (at most 31 days)")
	@GetMapping("/{id}/schedule")
	@Transactional(readOnly = true)
	public List<BookingResponse> schedule(@PathVariable Long id, @RequestParam Instant from, @RequestParam Instant to) {
		if (!rooms.existsById(id)) {
			throw ApiException.notFound("Room", id);
		}
		if (!to.isAfter(from) || Duration.between(from, to).compareTo(MAX_SCHEDULE_RANGE) > 0) {
			throw ApiException.badRequest("'to' must be after 'from' and at most 31 days later");
		}
		return bookings.findConfirmedInRange(id, from, to).stream().map(BookingResponse::of).toList();
	}

}
