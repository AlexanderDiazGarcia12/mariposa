package com.mariposa.biblioteca.aplicacion.servicios;

import com.mariposa.biblioteca.dominio.excepciones.CredencialesInvalidas;
import com.mariposa.biblioteca.dominio.modelo.Usuario;
import com.mariposa.biblioteca.dominio.puertos.entrada.AutenticarUsuarioCasoUso;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.CredencialesComando;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.ResultadoAutenticacion;
import com.mariposa.biblioteca.dominio.puertos.salida.CodificadorContrasena;
import com.mariposa.biblioteca.dominio.puertos.salida.ProveedorToken;
import com.mariposa.biblioteca.dominio.puertos.salida.RepositorioUsuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ServicioAutenticacion implements AutenticarUsuarioCasoUso {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioAutenticacion.class);

    private final RepositorioUsuario repositorioUsuario;
    private final CodificadorContrasena codificadorContrasena;
    private final ProveedorToken proveedorToken;

    public ServicioAutenticacion(
            RepositorioUsuario repositorioUsuario,
            CodificadorContrasena codificadorContrasena,
            ProveedorToken proveedorToken
    ) {
        this.repositorioUsuario = repositorioUsuario;
        this.codificadorContrasena = codificadorContrasena;
        this.proveedorToken = proveedorToken;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultadoAutenticacion iniciarSesion(CredencialesComando credenciales) {
        var usuario = repositorioUsuario.obtenerPorNombreUsuario(credenciales.nombreUsuario())
                .orElseThrow(() -> rechazar("usuario no existe"));
        if (!usuario.estaActivo()) {
            throw rechazar("usuario no activo");
        }
        if (!codificadorContrasena.coincide(credenciales.contrasenaPlana(), usuario.contrasenaEncriptada())) {
            throw rechazar("contraseña no coincide");
        }
        return emitirResultado(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public ResultadoAutenticacion refrescarToken(String tokenRefresco) {
        var claims = proveedorToken.validar(tokenRefresco);
        if (!claims.esRefresco()) {
            throw rechazar("token no es de refresco");
        }
        var usuario = obtenerUsuarioActivoOFallar(claims.idUsuario());
        return emitirResultado(usuario);
    }

    private Usuario obtenerUsuarioActivoOFallar(UUID idUsuario) {
        var usuario = repositorioUsuario.obtenerPorId(idUsuario)
                .orElseThrow(() -> rechazar("usuario del refresco no existe"));
        if (!usuario.estaActivo()) {
            throw rechazar("usuario del refresco no activo");
        }
        return usuario;
    }

    private ResultadoAutenticacion emitirResultado(Usuario usuario) {
        var acceso = proveedorToken.generarAcceso(usuario);
        var refresco = proveedorToken.generarRefresco(usuario);
        return new ResultadoAutenticacion(acceso.token(), refresco.token(), acceso.expiraEn(), usuario);
    }

    private CredencialesInvalidas rechazar(String motivo) {
        LOGGER.debug("Autenticación rechazada: {}", motivo);
        return new CredencialesInvalidas();
    }
}
