package com.mariposa.biblioteca.dominio.excepciones;

public sealed abstract class ExcepcionDominio extends RuntimeException
        permits LibroNoEncontrado,
                LibroNoEncontradoPorIsbn,
                UsuarioNoEncontrado,
                UsuarioNoEncontradoPorNombre,
                SinCopiasDisponibles,
                LibroYaExiste,
                UsuarioYaExiste,
                CredencialesInvalidas,
                OperacionInvalidaDominio,
                ValorInvalidoDominio,
                ServicioPrestamosNoDisponible,
                PrestamoRechazadoPorServicioB {

    protected ExcepcionDominio(String mensaje) {
        super(mensaje);
    }

    protected ExcepcionDominio(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
