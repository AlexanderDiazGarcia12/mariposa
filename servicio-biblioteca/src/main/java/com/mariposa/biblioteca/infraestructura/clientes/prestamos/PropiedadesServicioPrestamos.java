package com.mariposa.biblioteca.infraestructura.clientes.prestamos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "servicio.prestamos")
public record PropiedadesServicioPrestamos(
        @NotBlank String urlBase,
        @Positive long timeoutConexionMs,
        @Positive long timeoutLecturaMs
) {

    public PropiedadesServicioPrestamos {
        if (timeoutConexionMs <= 0) {
            timeoutConexionMs = 2000;
        }
        if (timeoutLecturaMs <= 0) {
            timeoutLecturaMs = 5000;
        }
    }
}
