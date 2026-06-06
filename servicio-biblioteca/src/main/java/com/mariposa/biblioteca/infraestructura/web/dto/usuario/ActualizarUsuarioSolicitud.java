package com.mariposa.biblioteca.infraestructura.web.dto.usuario;

import com.mariposa.biblioteca.dominio.modelo.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record ActualizarUsuarioSolicitud(
        @Email(message = "correoElectronico debe tener formato válido")
        String correoElectronico,

        Rol rol,

        @Size(min = 8, message = "contrasena debe tener al menos 8 caracteres")
        String contrasena
) {
}
