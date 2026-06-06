package com.mariposa.biblioteca.dominio.excepciones;

import com.mariposa.biblioteca.dominio.modelo.valor.Isbn;

public final class LibroYaExiste extends ExcepcionDominio {

    private final Isbn isbn;

    public LibroYaExiste(Isbn isbn) {
        super("Ya existe un libro registrado con ISBN %s".formatted(isbn.valor()));
        this.isbn = isbn;
    }

    public Isbn isbn() {
        return isbn;
    }
}
