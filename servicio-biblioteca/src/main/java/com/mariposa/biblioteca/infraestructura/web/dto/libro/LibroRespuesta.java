package com.mariposa.biblioteca.infraestructura.web.dto.libro;

import com.mariposa.biblioteca.dominio.modelo.Genero;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(
        name = "LibroRespuesta",
        description = "Representación pública de un libro del catálogo, incluyendo su disponibilidad."
)
public record LibroRespuesta(
        @Schema(
                description = "Identificador único del libro.",
                example = "1f4a2c8d-aaaa-4bcd-9012-1234567890ab",
                format = "uuid",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        UUID id,

        @Schema(
                description = "Título del libro.",
                example = "Cien años de soledad",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String titulo,

        @Schema(
                description = "Autor o autores del libro.",
                example = "Gabriel García Márquez",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String autor,

        @Schema(
                description = "ISBN único del libro.",
                example = "9780307474728",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String isbn,

        @Schema(
                description = "Año de publicación del libro.",
                example = "1967",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        int anioPublicacion,

        @Schema(
                description = "Género literario del libro.",
                example = "FICCION",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        Genero genero,

        @Schema(
                description = "Número total de copias del libro en la biblioteca.",
                example = "5",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        int copiasTotales,

        @Schema(
                description = "Número de copias actualmente disponibles para préstamo.",
                example = "3",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        int copiasDisponibles,

        @Schema(
                description = "Instante UTC ISO-8601 en que se creó el libro.",
                example = "2026-06-01T10:00:00Z",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        Instant fechaCreacion,

        @Schema(
                description = "Instante UTC ISO-8601 de la última actualización del libro.",
                example = "2026-06-05T14:30:00Z",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        Instant fechaActualizacion
) {
}
