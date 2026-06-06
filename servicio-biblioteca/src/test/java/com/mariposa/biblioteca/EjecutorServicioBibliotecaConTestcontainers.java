package com.mariposa.biblioteca;

import org.springframework.boot.SpringApplication;

public class EjecutorServicioBibliotecaConTestcontainers {

	public static void main(String[] args) {
		SpringApplication.from(ServicioBibliotecaApplication::main)
				.with(ConfiguracionTestcontainers.class)
				.run(args);
	}

}
