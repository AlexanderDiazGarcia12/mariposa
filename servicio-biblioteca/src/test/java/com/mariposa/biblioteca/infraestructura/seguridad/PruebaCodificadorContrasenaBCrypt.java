package com.mariposa.biblioteca.infraestructura.seguridad;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PruebaCodificadorContrasenaBCrypt {

    private CodificadorContrasenaBCrypt codificador;

    @BeforeEach
    void inicializar() {
        codificador = new CodificadorContrasenaBCrypt(new BCryptPasswordEncoder());
    }

    @Test
    @DisplayName("codificar produce un hash distinto al texto plano")
    void codificarProduceHashDistintoAlTextoPlano() {
        var contrasenaPlana = "Contrasena.Segura.123";

        var hash = codificador.codificar(contrasenaPlana);

        assertThat(hash).isNotEqualTo(contrasenaPlana);
        assertThat(hash).startsWith("$2");
    }

    @Test
    @DisplayName("coincide retorna verdadero cuando la contraseña es correcta")
    void coincideRetornaVerdaderoConContrasenaCorrecta() {
        var contrasenaPlana = "Contrasena.Segura.123";
        var hash = codificador.codificar(contrasenaPlana);

        assertThat(codificador.coincide(contrasenaPlana, hash)).isTrue();
    }

    @Test
    @DisplayName("coincide retorna falso cuando la contraseña es incorrecta")
    void coincideRetornaFalsoConContrasenaIncorrecta() {
        var hash = codificador.codificar("Contrasena.Original");

        assertThat(codificador.coincide("Otra.Contrasena", hash)).isFalse();
    }

    @Test
    @DisplayName("codificar produce hashes distintos para la misma entrada por salt aleatorio")
    void codificarProduceHashesDistintosParaMismaEntrada() {
        var contrasenaPlana = "Contrasena.Segura.123";

        var primerHash = codificador.codificar(contrasenaPlana);
        var segundoHash = codificador.codificar(contrasenaPlana);

        assertThat(primerHash).isNotEqualTo(segundoHash);
        assertThat(codificador.coincide(contrasenaPlana, primerHash)).isTrue();
        assertThat(codificador.coincide(contrasenaPlana, segundoHash)).isTrue();
    }

    @Test
    @DisplayName("coincide retorna falso ante entradas nulas")
    void coincideRetornaFalsoConEntradasNulas() {
        var hash = codificador.codificar("algo");

        assertThat(codificador.coincide(null, hash)).isFalse();
        assertThat(codificador.coincide("algo", null)).isFalse();
    }
}
