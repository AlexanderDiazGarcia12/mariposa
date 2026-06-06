package com.mariposa.biblioteca.dominio.excepciones;

public final class ServicioPrestamosNoDisponible extends ExcepcionDominio {

    public ServicioPrestamosNoDisponible(String mensaje) {
        super(mensaje);
    }

    public ServicioPrestamosNoDisponible(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
