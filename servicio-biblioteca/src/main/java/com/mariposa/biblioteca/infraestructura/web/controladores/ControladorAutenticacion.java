package com.mariposa.biblioteca.infraestructura.web.controladores;

import com.mariposa.biblioteca.dominio.puertos.entrada.AutenticarUsuarioCasoUso;
import com.mariposa.biblioteca.infraestructura.web.dto.autenticacion.AutenticacionRespuesta;
import com.mariposa.biblioteca.infraestructura.web.dto.autenticacion.IniciarSesionSolicitud;
import com.mariposa.biblioteca.infraestructura.web.dto.autenticacion.RefrescarTokenSolicitud;
import com.mariposa.biblioteca.infraestructura.web.mapeadores.MapeadorWebAutenticacion;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/autenticacion")
public class ControladorAutenticacion {

    private final AutenticarUsuarioCasoUso autenticarUsuarioCasoUso;
    private final MapeadorWebAutenticacion mapeadorWebAutenticacion;

    public ControladorAutenticacion(
            AutenticarUsuarioCasoUso autenticarUsuarioCasoUso,
            MapeadorWebAutenticacion mapeadorWebAutenticacion
    ) {
        this.autenticarUsuarioCasoUso = autenticarUsuarioCasoUso;
        this.mapeadorWebAutenticacion = mapeadorWebAutenticacion;
    }

    @PostMapping("/iniciar-sesion")
    public ResponseEntity<AutenticacionRespuesta> iniciarSesion(
            @Valid @RequestBody IniciarSesionSolicitud solicitud
    ) {
        var comando = mapeadorWebAutenticacion.aComandoCredenciales(solicitud);
        var resultado = autenticarUsuarioCasoUso.iniciarSesion(comando);
        return ResponseEntity.ok(mapeadorWebAutenticacion.aRespuesta(resultado));
    }

    @PostMapping("/refrescar")
    public ResponseEntity<AutenticacionRespuesta> refrescar(
            @Valid @RequestBody RefrescarTokenSolicitud solicitud
    ) {
        var resultado = autenticarUsuarioCasoUso.refrescarToken(solicitud.tokenRefresco());
        return ResponseEntity.ok(mapeadorWebAutenticacion.aRespuesta(resultado));
    }
}
