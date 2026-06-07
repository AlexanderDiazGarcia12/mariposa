package com.mariposa.biblioteca.infraestructura.seguridad;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "seguridad.limite-tasa.inicio-sesion")
public record PropiedadesLimiteTasaInicioSesion(
        @NotNull Boolean habilitado,
        @Min(1) int capacidad,
        @Min(1) int ventanaSegundos
) {

    public PropiedadesLimiteTasaInicioSesion {
        if (habilitado == null) {
            habilitado = Boolean.TRUE;
        }
    }
}
