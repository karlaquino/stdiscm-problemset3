package com.garynation.problemset3.consumer_app;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties
public class ConsumerAppApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(ConsumerAppApplication.class, args);
	}

}