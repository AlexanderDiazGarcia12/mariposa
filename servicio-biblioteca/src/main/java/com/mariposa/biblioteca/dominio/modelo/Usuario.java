package com.mariposa.biblioteca.dominio.modelo;

import com.mariposa.biblioteca.dominio.excepciones.ValorInvalidoDominio;
import com.mariposa.biblioteca.dominio.modelo.valor.CorreoElectronico;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Usuario(
        UUID id,
        String nombreUsuario,
        CorreoElectronico correoElectronico,
        String contrasenaEncriptada,
        Rol rol,
        EstadoUsuario estado,
        Instant fechaCreacion,
        Instant fechaActualizacion
) {

    private static final int LONGITUD_MINIMA_NOMBRE = 3;
    private static final int LONGITUD_MAXIMA_NOMBRE = 60;

    public Usuario {
        Objects.requireNonNull(id, "id no puede ser nulo");
        Objects.requireNonNull(correoElectronico, "correoElectronico no puede ser nulo");
        Objects.requireNonNull(rol, "rol no puede ser nulo");
        Objects.requireNonNull(estado, "estado no puede ser nulo");
        Objects.requireNonNull(fechaCreacion, "fechaCreacion no puede ser nula");
        Objects.requireNonNull(fechaActualizacion, "fechaActualizacion no puede ser nula");
        validarNombreUsuario(nombreUsuario);
        validarContrasenaEncriptada(contrasenaEncriptada);
    }

    public Usuario cambiarContrasena(String nuevaContrasenaEncriptada) {
        validarContrasenaEncriptada(nuevaContrasenaEncriptada);
        return new Usuario(
                id, nombreUsuario, correoElectronico, nuevaContrasenaEncriptada,
                rol, estado, fechaCreacion, Instant.now()
        );
    }

    public Usuario desactivar() {
        if (estado == EstadoUsuario.INACTIVO) {
            return this;
        }
        return cambiarEstado(EstadoUsuario.INACTIVO);
    }

    public Usuario activar() {
        if (estado == EstadoUsuario.ACTIVO) {
            return this;
        }
        return cambiarEstado(EstadoUsuario.ACTIVO);
    }

    public Usuario bloquear() {
        if (estado == EstadoUsuario.BLOQUEADO) {
            return this;
        }
        return cambiarEstado(EstadoUsuario.BLOQUEADO);
    }

    public Usuario cambiarRol(Rol nuevoRol) {
        Objects.requireNonNull(nuevoRol, "nuevoRol no puede ser nulo");
        if (rol == nuevoRol) {
            return this;
        }
        return new Usuario(
                id, nombreUsuario, correoElectronico, contrasenaEncriptada,
                nuevoRol, estado, fechaCreacion, Instant.now()
        );
    }

    public Usuario cambiarCorreo(CorreoElectronico nuevoCorreo) {
        Objects.requireNonNull(nuevoCorreo, "nuevoCorreo no puede ser nulo");
        if (correoElectronico.equals(nuevoCorreo)) {
            return this;
        }
        return new Usuario(
                id, nombreUsuario, nuevoCorreo, contrasenaEncriptada,
                rol, estado, fechaCreacion, Instant.now()
        );
    }

    public boolean estaActivo() {
        return estado == EstadoUsuario.ACTIVO;
    }

    public static Usuario nuevo(
            UUID id,
            String nombreUsuario,
            CorreoElectronico correoElectronico,
            String contrasenaEncriptada,
            Rol rol
    ) {
        var ahora = Instant.now();
        return new Usuario(
                id, nombreUsuario, correoElectronico, contrasenaEncriptada,
                rol, EstadoUsuario.ACTIVO, ahora, ahora
        );
    }

    private Usuario cambiarEstado(EstadoUsuario nuevoEstado) {
        return new Usuario(
                id, nombreUsuario, correoElectronico, contrasenaEncriptada,
                rol, nuevoEstado, fechaCreacion, Instant.now()
        );
    }

    private static void validarNombreUsuario(String nombreUsuario) {
        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            throw new ValorInvalidoDominio("nombreUsuario", "no puede ser nulo o vacío");
        }
        var longitud = nombreUsuario.length();
        if (longitud < LONGITUD_MINIMA_NOMBRE || longitud > LONGITUD_MAXIMA_NOMBRE) {
            throw new ValorInvalidoDominio(
                    "nombreUsuario",
                    "debe tener entre %d y %d caracteres".formatted(LONGITUD_MINIMA_NOMBRE, LONGITUD_MAXIMA_NOMBRE)
            );
        }
    }

    private static void validarContrasenaEncriptada(String contrasenaEncriptada) {
        if (contrasenaEncriptada == null || contrasenaEncriptada.isBlank()) {
            throw new ValorInvalidoDominio("contrasenaEncriptada", "no puede ser nula o vacía");
        }
    }
}
