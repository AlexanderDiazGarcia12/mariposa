package com.mariposa.biblioteca.infraestructura.persistencia.mapeadores;

import com.mariposa.biblioteca.dominio.modelo.Usuario;
import com.mariposa.biblioteca.dominio.modelo.valor.CorreoElectronico;
import com.mariposa.biblioteca.infraestructura.persistencia.entidades.EntidadUsuario;
import org.springframework.stereotype.Component;

@Component
public class MapeadorUsuario {

    public Usuario aDominio(EntidadUsuario entidad) {
        return new Usuario(
                entidad.getId(),
                entidad.getNombreUsuario(),
                new CorreoElectronico(entidad.getCorreoElectronico()),
                entidad.getContrasenaEncriptada(),
                entidad.getRol(),
                entidad.getEstado(),
                entidad.getFechaCreacion(),
                entidad.getFechaActualizacion()
        );
    }

    public EntidadUsuario aEntidad(Usuario usuario) {
        return new EntidadUsuario(
                usuario.id(),
                usuario.nombreUsuario(),
                usuario.correoElectronico().valor(),
                usuario.contrasenaEncriptada(),
                usuario.rol(),
                usuario.estado(),
                usuario.fechaCreacion(),
                usuario.fechaActualizacion()
        );
    }
}
