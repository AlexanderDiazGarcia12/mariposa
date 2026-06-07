package com.mariposa.biblioteca.infraestructura.web.dto.prestamo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

@Schema(
        name = "RegistrarPrestamoSolicitud",
        description = "Solicitud para registrar un préstamo. El identificador del usuario se toma del token JWT."
)
public record RegistrarPrestamoSolicitud(
        @Schema(description = "Identificador del libro a prestar.", example = "1f4a2c8d-aaaa-4bcd-9012-1234567890ab", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "idLibro es obligatorio")
        UUID idLibro,

        @Schema(description = "Fecha de inicio del préstamo (ISO yyyy-MM-dd).", example = "2026-06-06", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "fechaPrestamo es obligatoria")
        @FutureOrPresent(message = "fechaPrestamo no puede estar en el pasado")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate fechaPrestamo,

        @Schema(description = "Fecha estimada de devolución (ISO yyyy-MM-dd).", example = "2026-06-20", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "fechaDevolucionEstimada es obligatoria")
        @FutureOrPresent(message = "fechaDevolucionEstimada no puede estar en el pasado")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate fechaDevolucionEstimada
) {
}
