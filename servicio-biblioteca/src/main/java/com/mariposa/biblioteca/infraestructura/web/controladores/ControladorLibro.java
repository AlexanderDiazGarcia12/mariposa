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
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<LibroRespuesta> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarLibroSolicitud solicitud
    ) {
        var comando = mapeadorWebLibro.aComandoActualizar(id, solicitud);
        var actualizado = gestionarLibrosCasoUso.actualizar(comando);
        return ResponseEntity.ok(mapeadorWebLibro.aRespuesta(actualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        gestionarLibrosCasoUso.eliminar(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<LibroRespuesta> obtenerPorId(@PathVariable UUID id) {
        var libro = gestionarLibrosCasoUso.obtenerPorId(id);
        return ResponseEntity.ok(mapeadorWebLibro.aRespuesta(libro));
    }

    @GetMapping("/por-isbn/{isbn}")
    public ResponseEntity<LibroRespuesta> obtenerPorIsbn(@PathVariable String isbn) {
        var libro = gestionarLibrosCasoUso.obtenerPorIsbn(Isbn.desTexto(isbn));
        return ResponseEntity.ok(mapeadorWebLibro.aRespuesta(libro));
    }

    @GetMapping
    public ResponseEntity<PaginaRespuesta<LibroRespuesta>> listar(
            @RequestParam(required = false) String autor,
            @RequestParam(required = false) Genero genero,
            @RequestParam(required = false) Boolean conDisponibilidad,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
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
