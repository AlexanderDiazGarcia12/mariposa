package com.mariposa.biblioteca.infraestructura.web.controladores;

import com.mariposa.biblioteca.dominio.modelo.Genero;
import com.mariposa.biblioteca.dominio.modelo.consultas.FiltroLibros;
import com.mariposa.biblioteca.dominio.modelo.consultas.OrdenLibro;
import com.mariposa.biblioteca.dominio.modelo.consultas.Paginacion;
import com.mariposa.biblioteca.dominio.modelo.valor.Isbn;
import com.mariposa.biblioteca.dominio.puertos.entrada.GestionarLibrosCasoUso;
import com.mariposa.biblioteca.infraestructura.web.dto.comun.PaginaRespuesta;
import com.mariposa.biblioteca.infraestructura.web.dto.libro.ActualizarLibroSolicitud;
import com.mariposa.biblioteca.infraestructura.web.dto.libro.CrearLibroSolicitud;
import com.mariposa.biblioteca.infraestructura.web.dto.libro.LibroRespuesta;
import com.mariposa.biblioteca.infraestructura.web.mapeadores.MapeadorWebLibro;
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
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/libros")
@Tag(
        name = "Libros",
        description = "Catálogo de libros: consulta para cualquier usuario autenticado y escritura para ADMIN."
)
@SecurityRequirement(name = "bearerAuth")
public class ControladorLibro {

    private final GestionarLibrosCasoUso gestionarLibrosCasoUso;
    private final MapeadorWebLibro mapeadorWebLibro;

    public ControladorLibro(
            GestionarLibrosCasoUso gestionarLibrosCasoUso,
            MapeadorWebLibro mapeadorWebLibro
    ) {
        this.gestionarLibrosCasoUso = gestionarLibrosCasoUso;
        this.mapeadorWebLibro = mapeadorWebLibro;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Crea un nuevo libro en el catálogo",
            description = "Requiere rol ADMIN. El ISBN debe ser único."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Libro creado. Header Location apunta al recurso.",
                    content = @Content(schema = @Schema(implementation = LibroRespuesta.class))
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
                    description = "El usuario autenticado no tiene rol ADMIN.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "ISBN duplicado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Valor inválido de dominio (por ejemplo formato de ISBN).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<LibroRespuesta> crear(
            @Valid @RequestBody CrearLibroSolicitud solicitud,
            UriComponentsBuilder constructorUri
    ) {
        var comando = mapeadorWebLibro.aComandoCrear(solicitud);
        var creado = gestionarLibrosCasoUso.crear(comando);
        var respuesta = mapeadorWebLibro.aRespuesta(creado);
        URI ubicacion = constructorUri.path("/api/v1/libros/{id}").buildAndExpand(respuesta.id()).toUri();
        return ResponseEntity.created(ubicacion).body(respuesta);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Actualiza los datos editables de un libro",
            description = "Requiere rol ADMIN. No permite cambiar el ISBN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Libro actualizado.",
                    content = @Content(schema = @Schema(implementation = LibroRespuesta.class))
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
                    description = "El usuario autenticado no tiene rol ADMIN.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Libro no encontrado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Valor inválido de dominio.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<LibroRespuesta> actualizar(
            @Parameter(description = "Identificador único del libro.", example = "1f4a2c8d-aaaa-4bcd-9012-1234567890ab")
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarLibroSolicitud solicitud
    ) {
        var comando = mapeadorWebLibro.aComandoActualizar(id, solicitud);
        var actualizado = gestionarLibrosCasoUso.actualizar(comando);
        return ResponseEntity.ok(mapeadorWebLibro.aRespuesta(actualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Elimina un libro del catálogo",
            description = "Requiere rol ADMIN. La operación falla si el libro tiene préstamos vigentes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Libro eliminado."),
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
                    description = "Libro no encontrado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "No se puede eliminar por estado del recurso (por ejemplo préstamos vigentes).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "Identificador único del libro.", example = "1f4a2c8d-aaaa-4bcd-9012-1234567890ab")
            @PathVariable UUID id
    ) {
        gestionarLibrosCasoUso.eliminar(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Obtiene un libro por id",
            description = "Cualquier usuario autenticado puede consultar el catálogo."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Libro encontrado.",
                    content = @Content(schema = @Schema(implementation = LibroRespuesta.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente o inválido.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Libro no encontrado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<LibroRespuesta> obtenerPorId(
            @Parameter(description = "Identificador único del libro.", example = "1f4a2c8d-aaaa-4bcd-9012-1234567890ab")
            @PathVariable UUID id
    ) {
        var libro = gestionarLibrosCasoUso.obtenerPorId(id);
        return ResponseEntity.ok(mapeadorWebLibro.aRespuesta(libro));
    }

    @GetMapping("/por-isbn/{isbn}")
    @Operation(
            summary = "Obtiene un libro por su ISBN",
            description = "Cualquier usuario autenticado puede consultar el catálogo por ISBN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Libro encontrado.",
                    content = @Content(schema = @Schema(implementation = LibroRespuesta.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente o inválido.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Libro no encontrado por ISBN.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "ISBN con formato inválido.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<LibroRespuesta> obtenerPorIsbn(
            @Parameter(description = "ISBN del libro (10 o 13 dígitos).", example = "9780307474728")
            @PathVariable String isbn
    ) {
        var libro = gestionarLibrosCasoUso.obtenerPorIsbn(Isbn.desTexto(isbn));
        return ResponseEntity.ok(mapeadorWebLibro.aRespuesta(libro));
    }

    @GetMapping
    @Operation(
            summary = "Lista libros con filtros y paginación",
            description = "Cualquier usuario autenticado puede listar el catálogo. "
                    + "Soporta filtros opcionales por autor, género y disponibilidad, "
                    + "más paginación y ordenamiento."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Página de libros que cumplen los filtros.",
                    content = @Content(schema = @Schema(implementation = PaginaRespuesta.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parámetros de consulta inválidos.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente o inválido.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<PaginaRespuesta<LibroRespuesta>> listar(
            @Parameter(description = "Filtro opcional por nombre parcial del autor (case-insensitive).", example = "garcía márquez")
            @RequestParam(required = false) String autor,
            @Parameter(description = "Filtro opcional por género literario.", example = "FICCION")
            @RequestParam(required = false) Genero genero,
            @Parameter(description = "Si es true, solo libros con copias disponibles. Si es false, solo agotados. Si se omite, sin filtro.", example = "true")
            @RequestParam(required = false) Boolean conDisponibilidad,
            @Parameter(description = "Número de página (base 0).", example = "0")
            @RequestParam(defaultValue = "0") int pagina,
            @Parameter(description = "Cantidad de elementos por página.", example = "20")
            @RequestParam(defaultValue = "20") int tamano,
            @Parameter(description = "Criterio de ordenamiento.", example = "POR_TITULO_ASC")
            @RequestParam(defaultValue = "POR_TITULO_ASC") OrdenLibro orden
    ) {
        var filtro = new FiltroLibros(
                Optional.ofNullable(autor),
                Optional.ofNullable(genero),
                Optional.ofNullable(conDisponibilidad),
                orden
        );
        var resultado = gestionarLibrosCasoUso.listar(filtro, new Paginacion(pagina, tamano));
        return ResponseEntity.ok(mapeadorWebLibro.aPaginaRespuesta(resultado));
    }
}
