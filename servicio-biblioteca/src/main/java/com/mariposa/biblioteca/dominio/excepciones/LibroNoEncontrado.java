package com.mariposa.biblioteca.dominio.excepciones;

import java.util.UUID;

public final class LibroNoEncontrado extends ExcepcionDominio {

    private final UUID idLibro;

    public LibroNoEncontrado(UUID idLibro) {
        super("No se encontró el libro con identificador %s".formatted(idLibro));
        this.idLibro = idLibro;
    }

    public UUID idLibro() {
        return idLibro;
    }
}
