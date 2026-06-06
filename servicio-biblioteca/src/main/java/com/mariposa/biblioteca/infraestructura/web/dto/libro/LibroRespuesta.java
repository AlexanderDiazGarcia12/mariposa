package com.mariposa.biblioteca.infraestructura.web.dto.libro;

import com.mariposa.biblioteca.dominio.modelo.Genero;

import java.time.Instant;
import java.util.UUID;

public record LibroRespuesta(
        UUID id,
        String titulo,
        String autor,
        String isbn,
        int anioPublicacion,
        Genero genero,
        int copiasTotales,
        int copiasDisponibles,
        Instant fechaCreacion,
        Instant fechaActualizacion
) {
}
