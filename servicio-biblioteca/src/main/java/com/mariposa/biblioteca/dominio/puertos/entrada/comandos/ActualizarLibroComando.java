package com.mariposa.biblioteca.dominio.puertos.entrada.comandos;

import com.mariposa.biblioteca.dominio.modelo.Genero;

import java.util.UUID;

public record ActualizarLibroComando(
        UUID id,
        String titulo,
        String autor,
        int anioPublicacion,
        Genero genero,
        int copiasTotales
) {
}
