package dev.yasinkilinc.booking.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.junit.jupiter.api.RepeatedTest;

import dev.yasinkilinc.booking.ApiTestSupport;
import dev.yasinkilinc.booking.auth.Role;
import dev.yasinkilinc.booking.room.Room;

/**
 * Many people click "book" for the same slot at the same moment. Exactly one must win, and everyone else
 * must get a clean 409. A naive check-then-insert lets several requests through; relying on the exclusion
 * constraint alone makes PostgreSQL abort some of them with deadlocks (500s).
 */
class ConcurrentBookingTest extends ApiTestSupport {

	private static final int PARALLEL_REQUESTS = 20;

	@RepeatedTest(10)
	void exactlyOneOfManySimultaneousRequestsWins() throws Exception {
		Room room = newRoom(8);
		Instant start = dayAfterTomorrowAt9();
		List<String> tokens = new ArrayList<>();
		for (int i = 0; i < PARALLEL_REQUESTS; i++) {
			tokens.add(tokenFor("racer-" + i, Role.USER));
		}

		List<Response> responses = bookAllAtOnce(room, start, tokens);

		Map<Integer, Long> byStatus = responses.stream()
			.collect(Collectors.groupingBy(Response::status, TreeMap::new, Collectors.counting()));
		System.out.printf("%d simultaneous requests, responses by status: %s%n", PARALLEL_REQUESTS, byStatus);

		// Only 201 and 409: a deadlock or any other server error would show up here as a 500
		assertThat(byStatus).containsOnlyKeys(201, 409).containsEntry(201, 1L);
		Response schedule = call("GET", "/api/rooms/" + room.getId() + "/schedule?from=" + start + "&to="
				+ start.plus(1, ChronoUnit.DAYS), tokens.getFirst(), null);
		assertThat(schedule.body()).hasSize(1);
	}

	private List<Response> bookAllAtOnce(Room room, Instant start, List<String> tokens) throws Exception {
		CountDownLatch go = new CountDownLatch(1);
		try (ExecutorService pool = Executors.newFixedThreadPool(tokens.size())) {
			List<Future<Response>> futures = new ArrayList<>();
			for (int i = 0; i < tokens.size(); i++) {
				String token = tokens.get(i);
				// Different but overlapping ranges, so this is not only an exact-duplicate check
				Instant end = start.plus(30 + 15L * (i % 4), ChronoUnit.MINUTES);
				futures.add(pool.submit(() -> {
					go.await();
					return call("POST", "/api/bookings", token, booking(room, start, end));
				}));
			}
			go.countDown();
			List<Response> responses = new ArrayList<>();
			for (Future<Response> future : futures) {
				responses.add(future.get());
			}
			return responses;
		}
	}

}
