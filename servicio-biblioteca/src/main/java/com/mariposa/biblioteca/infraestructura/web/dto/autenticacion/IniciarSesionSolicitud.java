package com.mariposa.biblioteca.infraestructura.web.dto.autenticacion;

import jakarta.validation.constraints.NotBlank;

public record IniciarSesionSolicitud(
        @NotBlank(message = "nombreUsuario es obligatorio") String nombreUsuario,
        @NotBlank(message = "contrasena es obligatoria") String contrasena
) {
}
