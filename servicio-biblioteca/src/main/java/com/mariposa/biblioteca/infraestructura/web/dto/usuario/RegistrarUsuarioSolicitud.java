package com.mariposa.biblioteca.infraestructura.web.dto.usuario;

import com.mariposa.biblioteca.dominio.modelo.Rol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(
        name = "RegistrarUsuarioSolicitud",
        description = "Solicitud para registrar un nuevo usuario en el sistema."
)
public record RegistrarUsuarioSolicitud(
        @Schema(
                description = "Nombre de usuario único entre 3 y 60 caracteres.",
                example = "juan.perez",
                minLength = 3,
                maxLength = 60,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "nombreUsuario es obligatorio")
        @Size(min = 3, max = 60, message = "nombreUsuario debe tener entre 3 y 60 caracteres")
        String nombreUsuario,

        @Schema(
                description = "Correo electrónico válido del usuario.",
                example = "juan.perez@correo.com",
                format = "email",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "correoElectronico es obligatorio")
        @Email(message = "correoElectronico debe tener formato válido")
        String correoElectronico,

        @Schema(
                description = "Contraseña en texto plano de mínimo 8 caracteres.",
                example = "Mariposa2025!",
                minLength = 8,
                format = "password",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "contrasena es obligatoria")
        @Size(min = 8, message = "contrasena debe tener al menos 8 caracteres")
        String contrasena,

        @Schema(
                description = "Rol asignado al usuario (LECTOR o ADMIN).",
                example = "LECTOR",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "rol es obligatorio")
        Rol rol
) {
}
