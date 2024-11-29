package ru.iguana.calculator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class CalculatorApplication {
	public static void main(String[] args) {
		log.info("Application start");
		SpringApplication.run(CalculatorApplication.class, args);
	}
}
