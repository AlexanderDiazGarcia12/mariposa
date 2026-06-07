package com.mariposa.biblioteca.infraestructura.seguridad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "seguridad.interno")
public record PropiedadesServicioInterno(
        @NotBlank @Size(min = 16, message = "El secreto interno debe tener mínimo 16 caracteres")
        String secreto
) {
}
