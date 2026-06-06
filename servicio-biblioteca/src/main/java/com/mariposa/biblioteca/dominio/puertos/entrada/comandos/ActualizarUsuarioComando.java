package com.mariposa.biblioteca.dominio.puertos.entrada.comandos;

import com.mariposa.biblioteca.dominio.modelo.Rol;
import com.mariposa.biblioteca.dominio.modelo.valor.CorreoElectronico;

import java.util.Optional;
import java.util.UUID;

public record ActualizarUsuarioComando(
        UUID id,
        Optional<CorreoElectronico> nuevoCorreoElectronico,
        Optional<Rol> nuevoRol,
        Optional<String> nuevaContrasenaPlana
) {

    public ActualizarUsuarioComando {
        nuevoCorreoElectronico = nuevoCorreoElectronico == null ? Optional.empty() : nuevoCorreoElectronico;
        nuevoRol = nuevoRol == null ? Optional.empty() : nuevoRol;
        nuevaContrasenaPlana = nuevaContrasenaPlana == null ? Optional.empty() : nuevaContrasenaPlana;
    }
}
