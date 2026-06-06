package com.mariposa.biblioteca.dominio.puertos.entrada;

import com.mariposa.biblioteca.dominio.modelo.Usuario;
import com.mariposa.biblioteca.dominio.modelo.consultas.PaginaResultado;
import com.mariposa.biblioteca.dominio.modelo.consultas.Paginacion;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.ActualizarUsuarioComando;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.RegistrarUsuarioComando;

import java.util.UUID;

public interface GestionarUsuariosCasoUso {

    Usuario registrar(RegistrarUsuarioComando comando);

    Usuario actualizar(ActualizarUsuarioComando comando);

    Usuario desactivar(UUID id);

    Usuario obtenerPorId(UUID id);

    PaginaResultado<Usuario> listar(Paginacion paginacion);
}
