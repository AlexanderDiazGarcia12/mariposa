package com.mariposa.biblioteca.infraestructura.seguridad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "seguridad.jwt")
public record PropiedadesJwt(
        @NotBlank @Size(min = 32, message = "La clave secreta debe tener mínimo 32 caracteres para HS256")
        String claveSecreta,
        @Positive long duracionAccesoMinutos,
        @Positive long duracionRefrescoDias,
        @NotBlank String emisor
) {
}
