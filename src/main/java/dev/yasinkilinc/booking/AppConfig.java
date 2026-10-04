package dev.yasinkilinc.booking;

import java.time.Clock;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	OpenAPI openApi() {
		SecurityScheme bearer = new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT");
		return new OpenAPI()
			.info(new Info().title("Room Booking API")
				.version("1.0.0")
				.description("Book meeting rooms without double bookings. "
						+ "Get a token from POST /api/auth/token and send it as a Bearer token."))
			.components(new Components().addSecuritySchemes("bearer", bearer))
			.addSecurityItem(new SecurityRequirement().addList("bearer"));
	}

}
