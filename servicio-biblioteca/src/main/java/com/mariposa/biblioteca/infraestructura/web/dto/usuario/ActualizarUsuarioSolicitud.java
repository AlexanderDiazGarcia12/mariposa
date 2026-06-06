package com.mariposa.biblioteca.infraestructura.web.dto.usuario;

import com.mariposa.biblioteca.dominio.modelo.Rol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Schema(
        name = "ActualizarUsuarioSolicitud",
        description = "Solicitud parcial para actualizar datos de un usuario existente. Todos los campos son opcionales."
)
public record ActualizarUsuarioSolicitud(
        @Schema(
                description = "Nuevo correo electrónico del usuario.",
                example = "nuevo.correo@correo.com",
                format = "email",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                nullable = true
        )
        @Email(message = "correoElectronico debe tener formato válido")
        String correoElectronico,

        @Schema(
                description = "Nuevo rol del usuario. Solo aplicable por ADMIN.",
                example = "ADMIN",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                nullable = true
        )
        Rol rol,

        @Schema(
                description = "Nueva contraseña en texto plano de mínimo 8 caracteres.",
                example = "NuevaClave2026!",
                minLength = 8,
                format = "password",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                nullable = true
        )
        @Size(min = 8, message = "contrasena debe tener al menos 8 caracteres")
        String contrasena
) {
}
