package com.garynation.problemset3.consumer_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ConsumerAppApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(ConsumerAppApplication.class, args);
		VideoHandler videoHandler = context.getBean(VideoHandler.class);
		videoHandler.startServer();
	}
}