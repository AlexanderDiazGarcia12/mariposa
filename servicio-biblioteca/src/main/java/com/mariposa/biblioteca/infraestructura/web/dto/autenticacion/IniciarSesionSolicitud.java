package com.mariposa.biblioteca.infraestructura.web.dto.autenticacion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(
        name = "IniciarSesionSolicitud",
        description = "Credenciales requeridas para iniciar sesión y emitir tokens JWT."
)
public record IniciarSesionSolicitud(
        @Schema(
                description = "Nombre de usuario único registrado en el sistema.",
                example = "admin",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "nombreUsuario es obligatorio") String nombreUsuario,

        @Schema(
                description = "Contraseña en texto plano (solo en tránsito, nunca persistida).",
                example = "Mariposa2025!",
                requiredMode = Schema.RequiredMode.REQUIRED,
                format = "password"
        )
        @NotBlank(message = "contrasena es obligatoria") String contrasena
) {
}
