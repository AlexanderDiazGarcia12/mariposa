package com.mariposa.biblioteca.infraestructura.web.controladores;

import com.mariposa.biblioteca.dominio.modelo.consultas.Paginacion;
import com.mariposa.biblioteca.dominio.puertos.entrada.GestionarUsuariosCasoUso;
import com.mariposa.biblioteca.infraestructura.web.dto.comun.PaginaRespuesta;
import com.mariposa.biblioteca.infraestructura.web.dto.usuario.ActualizarUsuarioSolicitud;
import com.mariposa.biblioteca.infraestructura.web.dto.usuario.RegistrarUsuarioSolicitud;
import com.mariposa.biblioteca.infraestructura.web.dto.usuario.UsuarioRespuesta;
import com.mariposa.biblioteca.infraestructura.web.mapeadores.MapeadorWebUsuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(
        name = "Usuarios",
        description = "Registro público y gestión administrada de usuarios."
)
public class ControladorUsuario {

    private final GestionarUsuariosCasoUso gestionarUsuariosCasoUso;
    private final MapeadorWebUsuario mapeadorWebUsuario;

    public ControladorUsuario(
            GestionarUsuariosCasoUso gestionarUsuariosCasoUso,
            MapeadorWebUsuario mapeadorWebUsuario
    ) {
        this.gestionarUsuariosCasoUso = gestionarUsuariosCasoUso;
        this.mapeadorWebUsuario = mapeadorWebUsuario;
    }

    @PostMapping
    @Operation(
            summary = "Registra un nuevo usuario",
            description = "Endpoint público para registrar usuarios. La contraseña se almacena con BCrypt."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Usuario creado. Header Location apunta al recurso.",
                    content = @Content(schema = @Schema(implementation = UsuarioRespuesta.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Cuerpo inválido o validación fallida.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Ya existe un usuario con el mismo nombreUsuario o correoElectronico.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Valor inválido de dominio (por ejemplo formato no soportado).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<UsuarioRespuesta> registrar(
            @Valid @RequestBody RegistrarUsuarioSolicitud solicitud,
            UriComponentsBuilder constructorUri
    ) {
        var comando = mapeadorWebUsuario.aComandoRegistrar(solicitud);
        var usuario = gestionarUsuariosCasoUso.registrar(comando);
        var respuesta = mapeadorWebUsuario.aRespuesta(usuario);
        URI ubicacion = constructorUri.path("/api/v1/usuarios/{id}").buildAndExpand(respuesta.id()).toUri();
        return ResponseEntity.created(ubicacion).body(respuesta);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Lista usuarios paginados",
            description = "Solo ADMIN. Devuelve una página de usuarios ordenados por fecha de creación descendente."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Página de usuarios.",
                    content = @Content(schema = @Schema(implementation = PaginaRespuesta.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente o inválido.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene rol ADMIN.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<PaginaRespuesta<UsuarioRespuesta>> listar(
            @Parameter(description = "Número de página (base 0).", example = "0")
            @RequestParam(defaultValue = "0") int pagina,
            @Parameter(description = "Cantidad de elementos por página.", example = "20")
            @RequestParam(defaultValue = "20") int tamano
    ) {
        var paginacion = new Paginacion(pagina, tamano);
        var resultado = gestionarUsuariosCasoUso.listar(paginacion);
        return ResponseEntity.ok(mapeadorWebUsuario.aPaginaRespuesta(resultado));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id.equals(authentication.principal.idUsuario())")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Obtiene un usuario por id",
            description = "Solo ADMIN o el propio usuario pueden consultar sus datos."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuario encontrado.",
                    content = @Content(schema = @Schema(implementation = UsuarioRespuesta.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente o inválido.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no puede consultar este recurso.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario no encontrado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<UsuarioRespuesta> obtenerPorId(
            @Parameter(description = "Identificador único del usuario.", example = "8b1c2f6e-9e12-4a3d-9bf3-7a3b7e7f8a01")
            @PathVariable UUID id
    ) {
        var usuario = gestionarUsuariosCasoUso.obtenerPorId(id);
        return ResponseEntity.ok(mapeadorWebUsuario.aRespuesta(usuario));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id.equals(authentication.principal.idUsuario())")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Actualiza un usuario",
            description = "Solo ADMIN o el propio usuario pueden actualizar sus datos. "
                    + "Cambios de rol solo aplican si el solicitante es ADMIN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuario actualizado.",
                    content = @Content(schema = @Schema(implementation = UsuarioRespuesta.class))
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
                    responseCode = "403",
                    description = "Acción no permitida para el principal autenticado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario no encontrado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflicto con correo electrónico ya registrado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<UsuarioRespuesta> actualizar(
            @Parameter(description = "Identificador único del usuario.", example = "8b1c2f6e-9e12-4a3d-9bf3-7a3b7e7f8a01")
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarUsuarioSolicitud solicitud
    ) {
        var comando = mapeadorWebUsuario.aComandoActualizar(id, solicitud);
        var usuario = gestionarUsuariosCasoUso.actualizar(comando);
        return ResponseEntity.ok(mapeadorWebUsuario.aRespuesta(usuario));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Desactiva un usuario",
            description = "Solo ADMIN. La desactivación es lógica: cambia el estado a INACTIVO."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario desactivado."),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente o inválido.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene rol ADMIN.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario no encontrado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<Void> desactivar(
            @Parameter(description = "Identificador único del usuario.", example = "8b1c2f6e-9e12-4a3d-9bf3-7a3b7e7f8a01")
            @PathVariable UUID id
    ) {
        gestionarUsuariosCasoUso.desactivar(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
