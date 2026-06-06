package com.mariposa.biblioteca.dominio.modelo.consultas;

import com.mariposa.biblioteca.dominio.modelo.Genero;

import java.util.Optional;

public record FiltroLibros(
        Optional<String> autor,
        Optional<Genero> genero,
        Optional<Boolean> conDisponibilidad,
        OrdenLibro orden
) {

    public FiltroLibros {
        autor = autor == null ? Optional.empty() : autor;
        genero = genero == null ? Optional.empty() : genero;
        conDisponibilidad = conDisponibilidad == null ? Optional.empty() : conDisponibilidad;
        orden = orden == null ? OrdenLibro.POR_TITULO_ASC : orden;
    }

    public static FiltroLibros sinFiltros() {
        return new FiltroLibros(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                OrdenLibro.POR_TITULO_ASC
        );
    }
}
