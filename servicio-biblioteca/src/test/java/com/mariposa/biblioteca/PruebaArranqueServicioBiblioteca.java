package com.mariposa.biblioteca;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(ConfiguracionTestcontainers.class)
class PruebaArranqueServicioBiblioteca {

	@Test
	void elContextoDeLaAplicacionDebeCargarSinErrores() {
	}

}
