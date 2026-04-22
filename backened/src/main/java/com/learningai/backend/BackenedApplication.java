package com.learningai.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BackenedApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackenedApplication.class, args);
	}

}
