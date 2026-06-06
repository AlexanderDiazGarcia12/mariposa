package com.mariposa.biblioteca.dominio.excepciones;

import java.util.UUID;

public final class SinCopiasDisponibles extends ExcepcionDominio {

    private final UUID idLibro;

    public SinCopiasDisponibles(UUID idLibro) {
        super("El libro %s no tiene copias disponibles para préstamo".formatted(idLibro));
        this.idLibro = idLibro;
    }

    public UUID idLibro() {
        return idLibro;
    }
}
