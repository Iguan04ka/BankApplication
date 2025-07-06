package ru.iguana.integrationroles;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class IntegrationRolesApplication {

	public static void main(String[] args) {
		SpringApplication.run(IntegrationRolesApplication.class, args);
	}

}
