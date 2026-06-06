package com.mariposa.biblioteca.infraestructura.persistencia.mapeadores;

import com.mariposa.biblioteca.dominio.modelo.Libro;
import com.mariposa.biblioteca.dominio.modelo.valor.Isbn;
import com.mariposa.biblioteca.infraestructura.persistencia.entidades.EntidadLibro;
import org.springframework.stereotype.Component;

@Component
public class MapeadorLibro {

    public Libro aDominio(EntidadLibro entidad) {
        return new Libro(
                entidad.getId(),
                entidad.getTitulo(),
                entidad.getAutor(),
                new Isbn(entidad.getIsbn()),
                entidad.getAnioPublicacion(),
                entidad.getGenero(),
                entidad.getCopiasTotales(),
                entidad.getCopiasDisponibles(),
                entidad.getFechaCreacion(),
                entidad.getFechaActualizacion()
        );
    }

    public EntidadLibro aEntidad(Libro libro) {
        return new EntidadLibro(
                libro.id(),
                libro.titulo(),
                libro.autor(),
                libro.isbn().valor(),
                libro.anioPublicacion(),
                libro.genero(),
                libro.copiasTotales(),
                libro.copiasDisponibles(),
                libro.fechaCreacion(),
                libro.fechaActualizacion()
        );
    }
}
