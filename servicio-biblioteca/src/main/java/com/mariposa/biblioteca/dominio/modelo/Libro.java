package com.mariposa.biblioteca.dominio.modelo;

import com.mariposa.biblioteca.dominio.excepciones.OperacionInvalidaDominio;
import com.mariposa.biblioteca.dominio.excepciones.SinCopiasDisponibles;
import com.mariposa.biblioteca.dominio.excepciones.ValorInvalidoDominio;
import com.mariposa.biblioteca.dominio.modelo.valor.Isbn;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record Libro(
        UUID id,
        String titulo,
        String autor,
        Isbn isbn,
        int anioPublicacion,
        Genero genero,
        int copiasTotales,
        int copiasDisponibles,
        Instant fechaCreacion,
        Instant fechaActualizacion
) {

    private static final int ANIO_MINIMO = 1000;
    private static final int LONGITUD_MAXIMA_TITULO = 255;
    private static final int LONGITUD_MAXIMA_AUTOR = 255;

    public Libro {
        Objects.requireNonNull(id, "id no puede ser nulo");
        Objects.requireNonNull(isbn, "isbn no puede ser nulo");
        Objects.requireNonNull(genero, "genero no puede ser nulo");
        Objects.requireNonNull(fechaCreacion, "fechaCreacion no puede ser nula");
        Objects.requireNonNull(fechaActualizacion, "fechaActualizacion no puede ser nula");
        validarTitulo(titulo);
        validarAutor(autor);
        validarAnio(anioPublicacion);
        validarCopias(copiasTotales, copiasDisponibles);
    }

    public Libro prestar() {
        if (copiasDisponibles <= 0) {
            throw new SinCopiasDisponibles(id);
        }
        return new Libro(
                id, titulo, autor, isbn, anioPublicacion, genero,
                copiasTotales, copiasDisponibles - 1,
                fechaCreacion, Instant.now()
        );
    }

    public Libro devolver() {
        if (copiasDisponibles >= copiasTotales) {
            throw new OperacionInvalidaDominio(
                    "No se pueden devolver más copias de las prestadas para el libro %s".formatted(id)
            );
        }
        return new Libro(
                id, titulo, autor, isbn, anioPublicacion, genero,
                copiasTotales, copiasDisponibles + 1,
                fechaCreacion, Instant.now()
        );
    }

    public Libro actualizarCatalogo(
            String nuevoTitulo,
            String nuevoAutor,
            int nuevoAnioPublicacion,
            Genero nuevoGenero,
            int nuevasCopiasTotales
    ) {
        var copiasEnPrestamo = copiasTotales - copiasDisponibles;
        if (nuevasCopiasTotales < copiasEnPrestamo) {
            throw new OperacionInvalidaDominio(
                    "Las nuevas copias totales (%d) no pueden ser menores a las copias prestadas (%d)"
                            .formatted(nuevasCopiasTotales, copiasEnPrestamo)
            );
        }
        var nuevasCopiasDisponibles = nuevasCopiasTotales - copiasEnPrestamo;
        return new Libro(
                id, nuevoTitulo, nuevoAutor, isbn, nuevoAnioPublicacion, nuevoGenero,
                nuevasCopiasTotales, nuevasCopiasDisponibles,
                fechaCreacion, Instant.now()
        );
    }

    public static Libro nuevo(
            UUID id,
            String titulo,
            String autor,
            Isbn isbn,
            int anioPublicacion,
            Genero genero,
            int copiasTotales
    ) {
        var ahora = Instant.now();
        return new Libro(
                id, titulo, autor, isbn, anioPublicacion, genero,
                copiasTotales, copiasTotales,
                ahora, ahora
        );
    }

    private static void validarTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            throw new ValorInvalidoDominio("titulo", "no puede ser nulo o vacío");
        }
        if (titulo.length() > LONGITUD_MAXIMA_TITULO) {
            throw new ValorInvalidoDominio("titulo", "supera la longitud máxima permitida");
        }
    }

    private static void validarAutor(String autor) {
        if (autor == null || autor.isBlank()) {
            throw new ValorInvalidoDominio("autor", "no puede ser nulo o vacío");
        }
        if (autor.length() > LONGITUD_MAXIMA_AUTOR) {
            throw new ValorInvalidoDominio("autor", "supera la longitud máxima permitida");
        }
    }

    private static void validarAnio(int anioPublicacion) {
        var anioActual = LocalDate.now().getYear();
        if (anioPublicacion < ANIO_MINIMO || anioPublicacion > anioActual) {
            throw new ValorInvalidoDominio(
                    "anioPublicacion",
                    "debe estar entre %d y %d".formatted(ANIO_MINIMO, anioActual)
            );
        }
    }

    private static void validarCopias(int copiasTotales, int copiasDisponibles) {
        if (copiasTotales < 0) {
            throw new ValorInvalidoDominio("copiasTotales", "no puede ser negativa");
        }
        if (copiasDisponibles < 0) {
            throw new ValorInvalidoDominio("copiasDisponibles", "no puede ser negativa");
        }
        if (copiasDisponibles > copiasTotales) {
            throw new ValorInvalidoDominio(
                    "copiasDisponibles",
                    "no puede ser mayor que copiasTotales"
            );
        }
    }
}
