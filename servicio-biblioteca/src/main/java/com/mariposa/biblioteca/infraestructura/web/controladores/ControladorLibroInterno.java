package com.mariposa.biblioteca.infraestructura.web.controladores;

import com.mariposa.biblioteca.dominio.modelo.Libro;
import com.mariposa.biblioteca.dominio.puertos.entrada.GestionarLibrosCasoUso;
import com.mariposa.biblioteca.infraestructura.web.dto.interno.LibroInternoRespuesta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/libros")
@Tag(
        name = "Servicios Internos",
        description = "Endpoints de comunicación máquina-a-máquina entre microservicios. "
                + "Requieren el header X-Servicio-Interno con el secreto compartido."
)
@PreAuthorize("hasRole('SERVICIO_INTERNO')")
public class ControladorLibroInterno {

    private final GestionarLibrosCasoUso gestionarLibrosCasoUso;

    public ControladorLibroInterno(GestionarLibrosCasoUso gestionarLibrosCasoUso) {
        this.gestionarLibrosCasoUso = gestionarLibrosCasoUso;
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Obtiene un libro por id para uso interno entre servicios",
            description = "Consulta de catálogo destinada al servicio de préstamos para validar disponibilidad. "
                    + "La autenticación se realiza mediante el header X-Servicio-Interno."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Libro encontrado.",
                    content = @Content(schema = @Schema(implementation = LibroInternoRespuesta.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Header X-Servicio-Interno ausente o inválido.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Libro no encontrado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<LibroInternoRespuesta> obtenerPorId(
            @Parameter(description = "Identificador único del libro.", example = "1f4a2c8d-aaaa-4bcd-9012-1234567890ab")
            @PathVariable UUID id
    ) {
        var libro = gestionarLibrosCasoUso.obtenerPorId(id);
        return ResponseEntity.ok(aRespuestaInterna(libro));
    }

    private static LibroInternoRespuesta aRespuestaInterna(Libro libro) {
        return new LibroInternoRespuesta(
                libro.id(),
                libro.titulo(),
                libro.autor(),
                libro.isbn().valor(),
                libro.anioPublicacion(),
                libro.genero(),
                libro.copiasTotales(),
                libro.copiasDisponibles()
        );
    }
}
