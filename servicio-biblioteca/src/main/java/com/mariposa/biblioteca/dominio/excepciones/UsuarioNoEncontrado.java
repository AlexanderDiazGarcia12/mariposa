package com.mariposa.biblioteca.dominio.excepciones;

import java.util.UUID;

public final class UsuarioNoEncontrado extends ExcepcionDominio {

    private final UUID idUsuario;

    public UsuarioNoEncontrado(UUID idUsuario) {
        super("No se encontró el usuario con identificador %s".formatted(idUsuario));
        this.idUsuario = idUsuario;
    }

    public UUID idUsuario() {
        return idUsuario;
    }
}
