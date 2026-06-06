package com.mariposa.biblioteca.infraestructura.web.dto.libro;

import com.mariposa.biblioteca.dominio.modelo.Genero;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(
        name = "ActualizarLibroSolicitud",
        description = "Solicitud para actualizar los datos editables de un libro existente. Requiere rol ADMIN. El ISBN no se modifica."
)
public record ActualizarLibroSolicitud(
        @Schema(
                description = "Nuevo título del libro.",
                example = "Cien años de soledad — Edición conmemorativa",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "titulo es obligatorio") String titulo,

        @Schema(
                description = "Nuevo autor o autores del libro.",
                example = "Gabriel García Márquez",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "autor es obligatorio") String autor,

        @Schema(
                description = "Nuevo año de publicación entre 1000 y 2100.",
                example = "2007",
                minimum = "1000",
                maximum = "2100",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @Min(value = 1000, message = "anioPublicacion debe ser >= 1000")
        @Max(value = 2100, message = "anioPublicacion debe ser <= 2100")
        int anioPublicacion,

        @Schema(
                description = "Nuevo género literario del libro.",
                example = "FICCION",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "genero es obligatorio") Genero genero,

        @Schema(
                description = "Nuevo número total de copias del libro. Debe ser >= a las copias actualmente prestadas.",
                example = "8",
                minimum = "0",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @PositiveOrZero(message = "copiasTotales debe ser >= 0") int copiasTotales
) {
}
