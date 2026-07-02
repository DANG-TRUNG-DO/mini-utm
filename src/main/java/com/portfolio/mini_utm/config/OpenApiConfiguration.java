package com.portfolio.mini_utm.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI miniUtmOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mini UTM REST API")
                        .version("v1")
                        .description("REST API for managing drones, flight missions, geofences, "
                                + "telemetry and operational alerts.")
                        .contact(new Contact()
                                .name("Mini UTM maintainers")
                                .url("https://github.com/DANG-TRUNG-DO/mini-utm")));
    }
}
