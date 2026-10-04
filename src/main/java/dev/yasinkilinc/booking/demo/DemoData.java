package dev.yasinkilinc.booking.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dev.yasinkilinc.booking.auth.Role;
import dev.yasinkilinc.booking.auth.UserAccount;
import dev.yasinkilinc.booking.auth.UserAccountRepository;
import dev.yasinkilinc.booking.room.Room;
import dev.yasinkilinc.booking.room.RoomRepository;

/** Demo users and rooms for trying the API locally. Only runs with the "demo" profile. */
@Component
@Profile("demo")
public class DemoData implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DemoData.class);

	private final UserAccountRepository users;

	private final RoomRepository rooms;

	private final PasswordEncoder passwordEncoder;

	private final String password;

	public DemoData(UserAccountRepository users, RoomRepository rooms, PasswordEncoder passwordEncoder,
			@Value("${booking.demo.password}") String password) {
		this.users = users;
		this.rooms = rooms;
		this.passwordEncoder = passwordEncoder;
		this.password = password;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (password.isBlank()) {
			throw new IllegalStateException("Set DEMO_PASSWORD to use the demo profile");
		}
		addUser("alice", Role.USER);
		addUser("bob", Role.USER);
		addUser("admin", Role.ADMIN);
		if (rooms.count() == 0) {
			rooms.save(new Room("Bosphorus", 8));
			rooms.save(new Room("Galata", 4));
			rooms.save(new Room("Cappadocia", 20));
		}
		log.info("Demo data ready: users alice, bob (USER) and admin (ADMIN), password from DEMO_PASSWORD");
	}

	private void addUser(String username, Role role) {
		if (!users.existsByUsername(username)) {
			users.save(new UserAccount(username, passwordEncoder.encode(password), role));
		}
	}

}
