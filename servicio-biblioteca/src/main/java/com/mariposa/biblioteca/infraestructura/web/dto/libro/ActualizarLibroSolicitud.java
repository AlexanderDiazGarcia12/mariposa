package com.mariposa.biblioteca.infraestructura.web.dto.libro;

import com.mariposa.biblioteca.dominio.modelo.Genero;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ActualizarLibroSolicitud(
        @NotBlank(message = "titulo es obligatorio") String titulo,
        @NotBlank(message = "autor es obligatorio") String autor,
        @Min(value = 1000, message = "anioPublicacion debe ser >= 1000")
        @Max(value = 2100, message = "anioPublicacion debe ser <= 2100")
        int anioPublicacion,
        @NotNull(message = "genero es obligatorio") Genero genero,
        @PositiveOrZero(message = "copiasTotales debe ser >= 0") int copiasTotales
) {
}
