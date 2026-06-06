package com.mariposa.biblioteca.infraestructura.web.mapeadores;

import com.mariposa.biblioteca.dominio.modelo.Usuario;
import com.mariposa.biblioteca.dominio.modelo.consultas.PaginaResultado;
import com.mariposa.biblioteca.dominio.modelo.valor.CorreoElectronico;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.ActualizarUsuarioComando;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.RegistrarUsuarioComando;
import com.mariposa.biblioteca.infraestructura.web.dto.comun.PaginaRespuesta;
import com.mariposa.biblioteca.infraestructura.web.dto.usuario.ActualizarUsuarioSolicitud;
import com.mariposa.biblioteca.infraestructura.web.dto.usuario.RegistrarUsuarioSolicitud;
import com.mariposa.biblioteca.infraestructura.web.dto.usuario.UsuarioRespuesta;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class MapeadorWebUsuario {

    public UsuarioRespuesta aRespuesta(Usuario usuario) {
        return new UsuarioRespuesta(
                usuario.id(),
                usuario.nombreUsuario(),
                usuario.correoElectronico().valor(),
                usuario.rol(),
                usuario.estado(),
                usuario.fechaCreacion(),
                usuario.fechaActualizacion()
        );
    }

    public RegistrarUsuarioComando aComandoRegistrar(RegistrarUsuarioSolicitud solicitud) {
        return new RegistrarUsuarioComando(
                solicitud.nombreUsuario(),
                CorreoElectronico.desTexto(solicitud.correoElectronico()),
                solicitud.contrasena(),
                solicitud.rol()
        );
    }

    public ActualizarUsuarioComando aComandoActualizar(UUID id, ActualizarUsuarioSolicitud solicitud) {
        return new ActualizarUsuarioComando(
                id,
                opcionalDeCorreo(solicitud.correoElectronico()),
                Optional.ofNullable(solicitud.rol()),
                opcionalDeTexto(solicitud.contrasena())
        );
    }

    public PaginaRespuesta<UsuarioRespuesta> aPaginaRespuesta(PaginaResultado<Usuario> pagina) {
        var elementos = pagina.elementos().stream().map(this::aRespuesta).toList();
        return new PaginaRespuesta<>(
                elementos,
                pagina.paginaActual(),
                pagina.tamanoPagina(),
                pagina.totalElementos(),
                pagina.totalPaginas()
        );
    }

    private Optional<CorreoElectronico> opcionalDeCorreo(String correo) {
        return opcionalDeTexto(correo).map(CorreoElectronico::desTexto);
    }

    private Optional<String> opcionalDeTexto(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(valor);
    }
}
