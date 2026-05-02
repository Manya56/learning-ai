package com.learningai.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling  // ← add this
@ConfigurationPropertiesScan   // ← ADD THIS
public class BackenedApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackenedApplication.class, args);
	}

}
