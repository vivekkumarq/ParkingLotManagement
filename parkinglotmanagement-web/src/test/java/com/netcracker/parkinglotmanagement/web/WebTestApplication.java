package com.netcracker.parkinglotmanagement.web;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Configuration root for the {@code @WebMvcTest} slices in this module.
 *
 * <p>The web module has no application class of its own, and {@code @WebMvcTest}
 * needs one to anchor its slice. Only the MVC layer is auto-configured: the
 * services under the controllers are supplied as mocks, so no datasource, jOOQ or
 * Flyway is involved.
 */
@SpringBootApplication
public class WebTestApplication {
}
