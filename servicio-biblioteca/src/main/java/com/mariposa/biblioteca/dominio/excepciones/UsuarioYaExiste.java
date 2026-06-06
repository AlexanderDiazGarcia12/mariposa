package com.mariposa.biblioteca.dominio.excepciones;

public final class UsuarioYaExiste extends ExcepcionDominio {

    private final String identificador;

    public UsuarioYaExiste(String identificador) {
        super("Ya existe un usuario registrado con el identificador %s".formatted(identificador));
        this.identificador = identificador;
    }

    public String identificador() {
        return identificador;
    }
}
