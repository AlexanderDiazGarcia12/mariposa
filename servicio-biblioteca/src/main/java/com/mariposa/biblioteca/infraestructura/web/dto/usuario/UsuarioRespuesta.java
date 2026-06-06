package com.mariposa.biblioteca.infraestructura.web.dto.usuario;

import com.mariposa.biblioteca.dominio.modelo.EstadoUsuario;
import com.mariposa.biblioteca.dominio.modelo.Rol;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(
        name = "UsuarioRespuesta",
        description = "Representación pública de un usuario. Nunca incluye la contraseña."
)
public record UsuarioRespuesta(
        @Schema(
                description = "Identificador único del usuario.",
                example = "8b1c2f6e-9e12-4a3d-9bf3-7a3b7e7f8a01",
                format = "uuid",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        UUID id,

        @Schema(
                description = "Nombre de usuario único.",
                example = "juan.perez",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String nombreUsuario,

        @Schema(
                description = "Correo electrónico del usuario.",
                example = "juan.perez@correo.com",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String correoElectronico,

        @Schema(
                description = "Rol asignado al usuario.",
                example = "LECTOR",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        Rol rol,

        @Schema(
                description = "Estado actual del usuario.",
                example = "ACTIVO",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        EstadoUsuario estado,

        @Schema(
                description = "Instante UTC ISO-8601 en que se creó el usuario.",
                example = "2026-06-01T10:00:00Z",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        Instant fechaCreacion,

        @Schema(
                description = "Instante UTC ISO-8601 de la última actualización del usuario.",
                example = "2026-06-05T14:30:00Z",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        Instant fechaActualizacion
) {
}
