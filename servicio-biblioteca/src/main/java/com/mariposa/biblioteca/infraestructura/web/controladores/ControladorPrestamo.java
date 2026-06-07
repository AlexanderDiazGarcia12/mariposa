package com.mariposa.biblioteca.infraestructura.web.controladores;

import com.mariposa.biblioteca.dominio.puertos.entrada.RegistrarPrestamoCasoUso;
import com.mariposa.biblioteca.dominio.puertos.salida.ProveedorToken.ClaimsToken;
import com.mariposa.biblioteca.infraestructura.web.dto.prestamo.PrestamoRespuesta;
import com.mariposa.biblioteca.infraestructura.web.dto.prestamo.RegistrarPrestamoSolicitud;
import com.mariposa.biblioteca.infraestructura.web.mapeadores.MapeadorWebPrestamo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/prestamos")
@Tag(
        name = "Préstamos",
        description = "Registro de préstamos. El servicio orquesta la validación local de catálogo "
                + "y delega la creación del préstamo al servicio de préstamos (Servicio B)."
)
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
public class ControladorPrestamo {

    private final RegistrarPrestamoCasoUso registrarPrestamoCasoUso;
    private final MapeadorWebPrestamo mapeadorWebPrestamo;

    public ControladorPrestamo(
            RegistrarPrestamoCasoUso registrarPrestamoCasoUso,
            MapeadorWebPrestamo mapeadorWebPrestamo
    ) {
        this.registrarPrestamoCasoUso = registrarPrestamoCasoUso;
        this.mapeadorWebPrestamo = mapeadorWebPrestamo;
    }

    @PostMapping
    @Operation(
            summary = "Registra un préstamo para el usuario autenticado",
            description = "Valida usuario y libro en la base local, verifica disponibilidad y delega "
                    + "la persistencia del préstamo al Servicio B. Decrementa las copias disponibles "
                    + "tras la confirmación del Servicio B."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Préstamo registrado.",
                    content = @Content(schema = @Schema(implementation = PrestamoRespuesta.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Cuerpo inválido o validación fallida.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente o inválido.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario o libro no encontrado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Sin copias disponibles u operación inválida.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Préstamo rechazado por el Servicio B.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Servicio de préstamos no disponible.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<PrestamoRespuesta> registrar(
            @AuthenticationPrincipal ClaimsToken claims,
            @Valid @RequestBody RegistrarPrestamoSolicitud solicitud,
            UriComponentsBuilder constructorUri
    ) {
        var comando = mapeadorWebPrestamo.aComando(claims.idUsuario(), solicitud);
        var registrado = registrarPrestamoCasoUso.registrar(comando);
        var respuesta = mapeadorWebPrestamo.aRespuesta(registrado);
        URI ubicacion = constructorUri.path("/api/v1/prestamos/{id}").buildAndExpand(respuesta.idPrestamo()).toUri();
        return ResponseEntity.created(ubicacion).body(respuesta);
    }
}
