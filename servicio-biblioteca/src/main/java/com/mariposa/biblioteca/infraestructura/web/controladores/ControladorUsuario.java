package com.mariposa.biblioteca.infraestructura.web.controladores;

import com.mariposa.biblioteca.dominio.modelo.consultas.Paginacion;
import com.mariposa.biblioteca.dominio.puertos.entrada.GestionarUsuariosCasoUso;
import com.mariposa.biblioteca.infraestructura.web.dto.comun.PaginaRespuesta;
import com.mariposa.biblioteca.infraestructura.web.dto.usuario.ActualizarUsuarioSolicitud;
import com.mariposa.biblioteca.infraestructura.web.dto.usuario.RegistrarUsuarioSolicitud;
import com.mariposa.biblioteca.infraestructura.web.dto.usuario.UsuarioRespuesta;
import com.mariposa.biblioteca.infraestructura.web.mapeadores.MapeadorWebUsuario;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/usuarios")
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
    public ResponseEntity<PaginaRespuesta<UsuarioRespuesta>> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano
    ) {
        var paginacion = new Paginacion(pagina, tamano);
        var resultado = gestionarUsuariosCasoUso.listar(paginacion);
        return ResponseEntity.ok(mapeadorWebUsuario.aPaginaRespuesta(resultado));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id.equals(authentication.principal.idUsuario())")
    public ResponseEntity<UsuarioRespuesta> obtenerPorId(@PathVariable UUID id) {
        var usuario = gestionarUsuariosCasoUso.obtenerPorId(id);
        return ResponseEntity.ok(mapeadorWebUsuario.aRespuesta(usuario));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id.equals(authentication.principal.idUsuario())")
    public ResponseEntity<UsuarioRespuesta> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarUsuarioSolicitud solicitud
    ) {
        var comando = mapeadorWebUsuario.aComandoActualizar(id, solicitud);
        var usuario = gestionarUsuariosCasoUso.actualizar(comando);
        return ResponseEntity.ok(mapeadorWebUsuario.aRespuesta(usuario));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactivar(@PathVariable UUID id) {
        gestionarUsuariosCasoUso.desactivar(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
