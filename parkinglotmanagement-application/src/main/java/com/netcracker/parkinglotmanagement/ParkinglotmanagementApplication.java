package com.netcracker.parkinglotmanagement;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "Parking Lot Management"))
@ComponentScan(basePackages = {"com.netcracker.parkinglotmanagement.*"})
public class ParkinglotmanagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(ParkinglotmanagementApplication.class, args);
	}

}
