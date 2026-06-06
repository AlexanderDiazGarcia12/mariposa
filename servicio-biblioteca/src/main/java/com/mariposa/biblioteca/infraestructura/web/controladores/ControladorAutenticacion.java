package com.mariposa.biblioteca.infraestructura.web.controladores;

import com.mariposa.biblioteca.dominio.puertos.entrada.AutenticarUsuarioCasoUso;
import com.mariposa.biblioteca.infraestructura.web.dto.autenticacion.AutenticacionRespuesta;
import com.mariposa.biblioteca.infraestructura.web.dto.autenticacion.IniciarSesionSolicitud;
import com.mariposa.biblioteca.infraestructura.web.dto.autenticacion.RefrescarTokenSolicitud;
import com.mariposa.biblioteca.infraestructura.web.mapeadores.MapeadorWebAutenticacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/autenticacion")
@Tag(
        name = "Autenticación",
        description = "Endpoints públicos de emisión y renovación de tokens JWT."
)
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
    @Operation(
            summary = "Inicia sesión y emite tokens JWT",
            description = "Valida credenciales del usuario y emite par tokenAcceso + tokenRefresco. "
                    + "Errores opacos: cualquier fallo (usuario no existe, contraseña incorrecta, "
                    + "usuario inactivo) devuelve 401 con el mismo mensaje para evitar enumeración."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Autenticación exitosa. Retorna tokens y datos del usuario.",
                    content = @Content(schema = @Schema(implementation = AutenticacionRespuesta.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Cuerpo inválido o validación fallida.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credenciales inválidas (mensaje opaco).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<AutenticacionRespuesta> iniciarSesion(
            @Valid @RequestBody IniciarSesionSolicitud solicitud
    ) {
        var comando = mapeadorWebAutenticacion.aComandoCredenciales(solicitud);
        var resultado = autenticarUsuarioCasoUso.iniciarSesion(comando);
        return ResponseEntity.ok(mapeadorWebAutenticacion.aRespuesta(resultado));
    }

    @PostMapping("/refrescar")
    @Operation(
            summary = "Renueva tokens JWT a partir de un token de refresco",
            description = "Verifica la validez del tokenRefresco y emite un nuevo par de tokens "
                    + "(acceso + refresco). El token de refresco anterior queda invalidado conceptualmente."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Renovación exitosa.",
                    content = @Content(schema = @Schema(implementation = AutenticacionRespuesta.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Cuerpo inválido o tokenRefresco ausente.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token de refresco inválido, expirado o usuario inactivo.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<AutenticacionRespuesta> refrescar(
            @Valid @RequestBody RefrescarTokenSolicitud solicitud
    ) {
        var resultado = autenticarUsuarioCasoUso.refrescarToken(solicitud.tokenRefresco());
        return ResponseEntity.ok(mapeadorWebAutenticacion.aRespuesta(resultado));
    }
}
