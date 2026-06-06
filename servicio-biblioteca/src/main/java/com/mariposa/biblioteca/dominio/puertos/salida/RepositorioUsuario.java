package com.mariposa.biblioteca.dominio.puertos.salida;

import com.mariposa.biblioteca.dominio.modelo.Usuario;
import com.mariposa.biblioteca.dominio.modelo.consultas.PaginaResultado;
import com.mariposa.biblioteca.dominio.modelo.consultas.Paginacion;
import com.mariposa.biblioteca.dominio.modelo.valor.CorreoElectronico;

import java.util.Optional;
import java.util.UUID;

public interface RepositorioUsuario {

    Optional<Usuario> obtenerPorId(UUID id);

    Optional<Usuario> obtenerPorNombreUsuario(String nombreUsuario);

    Optional<Usuario> obtenerPorCorreo(CorreoElectronico correo);

    Usuario guardar(Usuario usuario);

    void eliminar(UUID id);

    PaginaResultado<Usuario> listar(Paginacion paginacion);

    boolean existePorNombreUsuario(String nombreUsuario);

    boolean existePorCorreo(CorreoElectronico correo);
}
