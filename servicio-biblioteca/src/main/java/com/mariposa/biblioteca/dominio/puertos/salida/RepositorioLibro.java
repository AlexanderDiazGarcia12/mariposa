package com.mariposa.biblioteca.dominio.puertos.salida;

import com.mariposa.biblioteca.dominio.modelo.Libro;
import com.mariposa.biblioteca.dominio.modelo.consultas.FiltroLibros;
import com.mariposa.biblioteca.dominio.modelo.consultas.PaginaResultado;
import com.mariposa.biblioteca.dominio.modelo.consultas.Paginacion;
import com.mariposa.biblioteca.dominio.modelo.valor.Isbn;

import java.util.Optional;
import java.util.UUID;

public interface RepositorioLibro {

    Optional<Libro> obtenerPorId(UUID id);

    Optional<Libro> obtenerPorIsbn(Isbn isbn);

    Libro guardar(Libro libro);

    void eliminar(UUID id);

    PaginaResultado<Libro> buscar(FiltroLibros filtro, Paginacion paginacion);

    boolean existePorIsbn(Isbn isbn);
}
