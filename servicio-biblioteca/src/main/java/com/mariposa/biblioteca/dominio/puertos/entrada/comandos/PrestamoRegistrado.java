package com.mariposa.biblioteca.dominio.puertos.entrada.comandos;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PrestamoRegistrado(
        UUID idPrestamo,
        UUID idUsuario,
        UUID idLibro,
        LocalDate fechaPrestamo,
        LocalDate fechaDevolucionEstimada,
        String estado,
        Instant registradoEn
) {
}
