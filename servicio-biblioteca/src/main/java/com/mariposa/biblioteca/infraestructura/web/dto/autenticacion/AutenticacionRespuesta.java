package com.mariposa.biblioteca.infraestructura.web.dto.autenticacion;

import com.mariposa.biblioteca.infraestructura.web.dto.usuario.UsuarioRespuesta;

import java.time.Instant;

public record AutenticacionRespuesta(
        String tokenAcceso,
        String tokenRefresco,
        Instant expiraEn,
        UsuarioRespuesta usuario
) {
}
