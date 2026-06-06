package com.mariposa.biblioteca.aplicacion.servicios;

import com.mariposa.biblioteca.dominio.excepciones.UsuarioNoEncontrado;
import com.mariposa.biblioteca.dominio.excepciones.UsuarioYaExiste;
import com.mariposa.biblioteca.dominio.modelo.Usuario;
import com.mariposa.biblioteca.dominio.modelo.consultas.PaginaResultado;
import com.mariposa.biblioteca.dominio.modelo.consultas.Paginacion;
import com.mariposa.biblioteca.dominio.puertos.entrada.GestionarUsuariosCasoUso;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.ActualizarUsuarioComando;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.RegistrarUsuarioComando;
import com.mariposa.biblioteca.dominio.puertos.salida.CodificadorContrasena;
import com.mariposa.biblioteca.dominio.puertos.salida.RepositorioUsuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ServicioGestionUsuarios implements GestionarUsuariosCasoUso {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioGestionUsuarios.class);

    private final RepositorioUsuario repositorioUsuario;
    private final CodificadorContrasena codificadorContrasena;

    public ServicioGestionUsuarios(
            RepositorioUsuario repositorioUsuario,
            CodificadorContrasena codificadorContrasena
    ) {
        this.repositorioUsuario = repositorioUsuario;
        this.codificadorContrasena = codificadorContrasena;
    }

    @Override
    @Transactional
    public Usuario registrar(RegistrarUsuarioComando comando) {
        if (repositorioUsuario.existePorNombreUsuario(comando.nombreUsuario())) {
            throw new UsuarioYaExiste(comando.nombreUsuario());
        }
        if (repositorioUsuario.existePorCorreo(comando.correoElectronico())) {
            throw new UsuarioYaExiste(comando.correoElectronico().valor());
        }
        var contrasenaEncriptada = codificadorContrasena.codificar(comando.contrasenaPlana());
        var nuevoUsuario = Usuario.nuevo(
                UUID.randomUUID(),
                comando.nombreUsuario(),
                comando.correoElectronico(),
                contrasenaEncriptada,
                comando.rol()
        );
        var guardado = repositorioUsuario.guardar(nuevoUsuario);
        LOGGER.info("Usuario registrado con id {} y nombre {}", guardado.id(), guardado.nombreUsuario());
        return guardado;
    }

    @Override
    @Transactional
    public Usuario actualizar(ActualizarUsuarioComando comando) {
        var usuarioExistente = repositorioUsuario.obtenerPorId(comando.id())
                .orElseThrow(() -> new UsuarioNoEncontrado(comando.id()));
        var conCorreo = aplicarNuevoCorreo(usuarioExistente, comando);
        var conRol = aplicarNuevoRol(conCorreo, comando);
        var conContrasena = aplicarNuevaContrasena(conRol, comando);
        var guardado = repositorioUsuario.guardar(conContrasena);
        LOGGER.info("Usuario actualizado con id {}", guardado.id());
        return guardado;
    }

    @Override
    @Transactional
    public Usuario desactivar(UUID id) {
        var usuarioExistente = repositorioUsuario.obtenerPorId(id)
                .orElseThrow(() -> new UsuarioNoEncontrado(id));
        var desactivado = usuarioExistente.desactivar();
        var guardado = repositorioUsuario.guardar(desactivado);
        LOGGER.info("Usuario desactivado con id {}", id);
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario obtenerPorId(UUID id) {
        return repositorioUsuario.obtenerPorId(id)
                .orElseThrow(() -> new UsuarioNoEncontrado(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResultado<Usuario> listar(Paginacion paginacion) {
        return repositorioUsuario.listar(paginacion);
    }

    private Usuario aplicarNuevoCorreo(Usuario usuario, ActualizarUsuarioComando comando) {
        return comando.nuevoCorreoElectronico()
                .map(usuario::cambiarCorreo)
                .orElse(usuario);
    }

    private Usuario aplicarNuevoRol(Usuario usuario, ActualizarUsuarioComando comando) {
        return comando.nuevoRol()
                .map(usuario::cambiarRol)
                .orElse(usuario);
    }

    private Usuario aplicarNuevaContrasena(Usuario usuario, ActualizarUsuarioComando comando) {
        return comando.nuevaContrasenaPlana()
                .map(codificadorContrasena::codificar)
                .map(usuario::cambiarContrasena)
                .orElse(usuario);
    }
}
