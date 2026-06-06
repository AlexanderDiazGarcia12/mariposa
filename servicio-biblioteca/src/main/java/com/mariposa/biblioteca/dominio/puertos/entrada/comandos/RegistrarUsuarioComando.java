package com.mariposa.biblioteca.dominio.puertos.entrada.comandos;

import com.mariposa.biblioteca.dominio.modelo.Rol;
import com.mariposa.biblioteca.dominio.modelo.valor.CorreoElectronico;

public record RegistrarUsuarioComando(
        String nombreUsuario,
        CorreoElectronico correoElectronico,
        String contrasenaPlana,
        Rol rol
) {
}
