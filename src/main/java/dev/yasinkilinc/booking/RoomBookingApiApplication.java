package dev.yasinkilinc.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RoomBookingApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(RoomBookingApiApplication.class, args);
	}

}
