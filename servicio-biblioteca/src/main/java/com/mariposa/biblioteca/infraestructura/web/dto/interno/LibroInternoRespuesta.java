package com.mariposa.biblioteca.infraestructura.web.dto.interno;

import com.mariposa.biblioteca.dominio.modelo.Genero;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(
        name = "LibroInternoRespuesta",
        description = "Representación interna de un libro para consumo por otros microservicios."
)
public record LibroInternoRespuesta(
        @Schema(description = "Identificador único del libro.", example = "1f4a2c8d-aaaa-4bcd-9012-1234567890ab", format = "uuid")
        UUID id,

        @Schema(description = "Título del libro.", example = "Cien años de soledad")
        String titulo,

        @Schema(description = "Autor o autores del libro.", example = "Gabriel García Márquez")
        String autor,

        @Schema(description = "ISBN del libro.", example = "9780307474728")
        String isbn,

        @Schema(description = "Año de publicación.", example = "1967")
        int anioPublicacion,

        @Schema(description = "Género literario.", example = "FICCION")
        Genero genero,

        @Schema(description = "Número total de copias.", example = "5")
        int copiasTotales,

        @Schema(description = "Número de copias actualmente disponibles para préstamo.", example = "3")
        int copiasDisponibles
) {
}
