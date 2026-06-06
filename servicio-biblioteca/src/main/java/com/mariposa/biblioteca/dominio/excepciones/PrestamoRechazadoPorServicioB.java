package com.mariposa.biblioteca.dominio.excepciones;

public final class PrestamoRechazadoPorServicioB extends ExcepcionDominio {

    private final String codigoError;

    public PrestamoRechazadoPorServicioB(String codigoError, String mensaje) {
        super("Servicio de préstamos rechazó la operación [%s]: %s".formatted(codigoError, mensaje));
        this.codigoError = codigoError;
    }

    public String codigoError() {
        return codigoError;
    }
}
