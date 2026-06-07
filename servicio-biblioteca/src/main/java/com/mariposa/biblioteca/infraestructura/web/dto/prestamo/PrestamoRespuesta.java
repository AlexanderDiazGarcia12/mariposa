package com.mariposa.biblioteca.infraestructura.web.dto.prestamo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(
        name = "PrestamoRespuesta",
        description = "Representación pública de un préstamo registrado por el servicio de préstamos."
)
public record PrestamoRespuesta(
        @Schema(description = "Identificador único del préstamo.", example = "33333333-aaaa-4444-bbbb-555555555555", format = "uuid")
        UUID idPrestamo,

        @Schema(description = "Identificador del usuario titular del préstamo.", example = "aaaaaaaa-1111-2222-3333-cccccccccccc", format = "uuid")
        UUID idUsuario,

        @Schema(description = "Identificador del libro prestado.", example = "1f4a2c8d-aaaa-4bcd-9012-1234567890ab", format = "uuid")
        UUID idLibro,

        @Schema(description = "Fecha de inicio del préstamo.", example = "2026-06-06")
        LocalDate fechaPrestamo,

        @Schema(description = "Fecha estimada de devolución.", example = "2026-06-20")
        LocalDate fechaDevolucionEstimada,

        @Schema(description = "Estado actual del préstamo.", example = "ACTIVO")
        String estado,

        @Schema(description = "Instante UTC ISO-8601 en que el préstamo quedó registrado.", example = "2026-06-06T10:15:00Z")
        Instant registradoEn
) {
}
