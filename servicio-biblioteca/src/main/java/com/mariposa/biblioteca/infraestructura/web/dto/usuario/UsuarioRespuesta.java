package com.mariposa.biblioteca.infraestructura.web.dto.usuario;

import com.mariposa.biblioteca.dominio.modelo.EstadoUsuario;
import com.mariposa.biblioteca.dominio.modelo.Rol;

import java.time.Instant;
import java.util.UUID;

public record UsuarioRespuesta(
        UUID id,
        String nombreUsuario,
        String correoElectronico,
        Rol rol,
        EstadoUsuario estado,
        Instant fechaCreacion,
        Instant fechaActualizacion
) {
}
