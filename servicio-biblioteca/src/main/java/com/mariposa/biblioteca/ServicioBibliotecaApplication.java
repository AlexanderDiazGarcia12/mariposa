package com.mariposa.biblioteca;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ServicioBibliotecaApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServicioBibliotecaApplication.class, args);
	}

}
