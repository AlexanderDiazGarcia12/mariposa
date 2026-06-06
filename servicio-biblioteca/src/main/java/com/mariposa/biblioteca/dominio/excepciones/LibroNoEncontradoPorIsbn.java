package com.mariposa.biblioteca.dominio.excepciones;

import com.mariposa.biblioteca.dominio.modelo.valor.Isbn;

public final class LibroNoEncontradoPorIsbn extends ExcepcionDominio {

    private final Isbn isbn;

    public LibroNoEncontradoPorIsbn(Isbn isbn) {
        super("No se encontró el libro con ISBN %s".formatted(isbn.valor()));
        this.isbn = isbn;
    }

    public Isbn isbn() {
        return isbn;
    }
}
