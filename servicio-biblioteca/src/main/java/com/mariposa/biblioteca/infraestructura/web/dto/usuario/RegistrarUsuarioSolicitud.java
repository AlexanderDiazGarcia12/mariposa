package com.mariposa.biblioteca.infraestructura.web.dto.usuario;

import com.mariposa.biblioteca.dominio.modelo.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrarUsuarioSolicitud(
        @NotBlank(message = "nombreUsuario es obligatorio")
        @Size(min = 3, max = 60, message = "nombreUsuario debe tener entre 3 y 60 caracteres")
        String nombreUsuario,

        @NotBlank(message = "correoElectronico es obligatorio")
        @Email(message = "correoElectronico debe tener formato válido")
        String correoElectronico,

        @NotBlank(message = "contrasena es obligatoria")
        @Size(min = 8, message = "contrasena debe tener al menos 8 caracteres")
        String contrasena,

        @NotNull(message = "rol es obligatorio")
        Rol rol
) {
}
