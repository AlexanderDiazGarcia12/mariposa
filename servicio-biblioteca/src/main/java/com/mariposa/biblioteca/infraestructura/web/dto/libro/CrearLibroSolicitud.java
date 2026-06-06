package com.mariposa.biblioteca.infraestructura.web.dto.libro;

import com.mariposa.biblioteca.dominio.modelo.Genero;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(
        name = "CrearLibroSolicitud",
        description = "Solicitud para crear un nuevo libro en el catálogo. Requiere rol ADMIN."
)
public record CrearLibroSolicitud(
        @Schema(
                description = "Título del libro.",
                example = "Cien años de soledad",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "titulo es obligatorio") String titulo,

        @Schema(
                description = "Autor o autores del libro.",
                example = "Gabriel García Márquez",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "autor es obligatorio") String autor,

        @Schema(
                description = "ISBN único del libro (10 o 13 dígitos sin guiones).",
                example = "9780307474728",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "isbn es obligatorio") String isbn,

        @Schema(
                description = "Año de publicación entre 1000 y 2100.",
                example = "1967",
                minimum = "1000",
                maximum = "2100",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @Min(value = 1000, message = "anioPublicacion debe ser >= 1000")
        @Max(value = 2100, message = "anioPublicacion debe ser <= 2100")
        int anioPublicacion,

        @Schema(
                description = "Género literario del libro.",
                example = "FICCION",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "genero es obligatorio") Genero genero,

        @Schema(
                description = "Número total de copias físicas del libro en la biblioteca.",
                example = "5",
                minimum = "0",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @PositiveOrZero(message = "copiasTotales debe ser >= 0") int copiasTotales
) {
}
