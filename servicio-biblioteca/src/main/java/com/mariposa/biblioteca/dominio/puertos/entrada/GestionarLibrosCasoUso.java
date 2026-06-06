package com.mariposa.biblioteca.dominio.puertos.entrada;

import com.mariposa.biblioteca.dominio.modelo.Libro;
import com.mariposa.biblioteca.dominio.modelo.consultas.FiltroLibros;
import com.mariposa.biblioteca.dominio.modelo.consultas.PaginaResultado;
import com.mariposa.biblioteca.dominio.modelo.consultas.Paginacion;
import com.mariposa.biblioteca.dominio.modelo.valor.Isbn;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.ActualizarLibroComando;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.CrearLibroComando;

import java.util.UUID;

public interface GestionarLibrosCasoUso {

    Libro crear(CrearLibroComando comando);

    Libro actualizar(ActualizarLibroComando comando);

    void eliminar(UUID id);

    Libro obtenerPorId(UUID id);

    Libro obtenerPorIsbn(Isbn isbn);

    PaginaResultado<Libro> listar(FiltroLibros filtro, Paginacion paginacion);
}
