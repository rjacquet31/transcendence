package com.transcendance.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;

@EnableScheduling
@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

		for (DotenvEntry envVar : dotenv.entries()) {
			System.setProperty(envVar.getKey(), envVar.getValue());
		}

		SpringApplication.run(BackendApplication.class, args);
	}

}
