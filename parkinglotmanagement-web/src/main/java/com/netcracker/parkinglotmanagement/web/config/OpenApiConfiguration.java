package com.netcracker.parkinglotmanagement.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Describes the API served at {@code /swagger-ui.html} and {@code /v3/api-docs}. */
@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI parkingLotOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Parking Lot Management API")
                .version("0.0.1-SNAPSHOT")
                .description("Vehicle entry and exit, concurrency-safe slot allocation, reservations, "
                        + "rate-card billing and operational reporting for a multi-block car park.")
                .contact(new Contact().name("Vivek Kumar"))
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")));
    }
}
