package com.mariposa.biblioteca.dominio.excepciones;

public final class UsuarioNoEncontradoPorNombre extends ExcepcionDominio {

    private final String nombreUsuario;

    public UsuarioNoEncontradoPorNombre(String nombreUsuario) {
        super("No se encontró el usuario con nombre %s".formatted(nombreUsuario));
        this.nombreUsuario = nombreUsuario;
    }

    public String nombreUsuario() {
        return nombreUsuario;
    }
}
