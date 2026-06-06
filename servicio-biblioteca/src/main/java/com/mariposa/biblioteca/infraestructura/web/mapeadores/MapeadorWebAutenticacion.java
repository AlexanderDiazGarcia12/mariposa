package com.mariposa.biblioteca.infraestructura.web.mapeadores;

import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.CredencialesComando;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.ResultadoAutenticacion;
import com.mariposa.biblioteca.infraestructura.web.dto.autenticacion.AutenticacionRespuesta;
import com.mariposa.biblioteca.infraestructura.web.dto.autenticacion.IniciarSesionSolicitud;
import org.springframework.stereotype.Component;

@Component
public class MapeadorWebAutenticacion {

    private final MapeadorWebUsuario mapeadorWebUsuario;

    public MapeadorWebAutenticacion(MapeadorWebUsuario mapeadorWebUsuario) {
        this.mapeadorWebUsuario = mapeadorWebUsuario;
    }

    public AutenticacionRespuesta aRespuesta(ResultadoAutenticacion resultado) {
        return new AutenticacionRespuesta(
                resultado.tokenAcceso(),
                resultado.tokenRefresco(),
                resultado.expiraEn(),
                mapeadorWebUsuario.aRespuesta(resultado.usuario())
        );
    }

    public CredencialesComando aComandoCredenciales(IniciarSesionSolicitud solicitud) {
        return new CredencialesComando(solicitud.nombreUsuario(), solicitud.contrasena());
    }
}
