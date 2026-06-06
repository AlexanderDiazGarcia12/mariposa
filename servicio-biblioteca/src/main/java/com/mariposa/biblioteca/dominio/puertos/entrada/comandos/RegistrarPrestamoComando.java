package com.mariposa.biblioteca.dominio.puertos.entrada.comandos;

import java.time.LocalDate;
import java.util.UUID;

public record RegistrarPrestamoComando(
        UUID idUsuario,
        UUID idLibro,
        LocalDate fechaPrestamo,
        LocalDate fechaDevolucionEstimada
) {
}
