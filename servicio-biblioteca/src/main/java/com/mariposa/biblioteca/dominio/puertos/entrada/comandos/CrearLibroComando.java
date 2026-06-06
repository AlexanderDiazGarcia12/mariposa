package com.mariposa.biblioteca.dominio.puertos.entrada.comandos;

import com.mariposa.biblioteca.dominio.modelo.Genero;
import com.mariposa.biblioteca.dominio.modelo.valor.Isbn;

public record CrearLibroComando(
        String titulo,
        String autor,
        Isbn isbn,
        int anioPublicacion,
        Genero genero,
        int copiasTotales
) {
}
