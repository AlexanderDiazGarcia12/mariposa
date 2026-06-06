package com.mariposa.biblioteca.infraestructura.web.mapeadores;

import com.mariposa.biblioteca.dominio.modelo.Libro;
import com.mariposa.biblioteca.dominio.modelo.consultas.PaginaResultado;
import com.mariposa.biblioteca.dominio.modelo.valor.Isbn;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.ActualizarLibroComando;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.CrearLibroComando;
import com.mariposa.biblioteca.infraestructura.web.dto.comun.PaginaRespuesta;
import com.mariposa.biblioteca.infraestructura.web.dto.libro.ActualizarLibroSolicitud;
import com.mariposa.biblioteca.infraestructura.web.dto.libro.CrearLibroSolicitud;
import com.mariposa.biblioteca.infraestructura.web.dto.libro.LibroRespuesta;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MapeadorWebLibro {

    public LibroRespuesta aRespuesta(Libro libro) {
        return new LibroRespuesta(
                libro.id(),
                libro.titulo(),
                libro.autor(),
                libro.isbn().valor(),
                libro.anioPublicacion(),
                libro.genero(),
                libro.copiasTotales(),
                libro.copiasDisponibles(),
                libro.fechaCreacion(),
                libro.fechaActualizacion()
        );
    }

    public CrearLibroComando aComandoCrear(CrearLibroSolicitud solicitud) {
        return new CrearLibroComando(
                solicitud.titulo(),
                solicitud.autor(),
                Isbn.desTexto(solicitud.isbn()),
                solicitud.anioPublicacion(),
                solicitud.genero(),
                solicitud.copiasTotales()
        );
    }

    public ActualizarLibroComando aComandoActualizar(UUID id, ActualizarLibroSolicitud solicitud) {
        return new ActualizarLibroComando(
                id,
                solicitud.titulo(),
                solicitud.autor(),
                solicitud.anioPublicacion(),
                solicitud.genero(),
                solicitud.copiasTotales()
        );
    }

    public PaginaRespuesta<LibroRespuesta> aPaginaRespuesta(PaginaResultado<Libro> pagina) {
        var elementos = pagina.elementos().stream().map(this::aRespuesta).toList();
        return new PaginaRespuesta<>(
                elementos,
                pagina.paginaActual(),
                pagina.tamanoPagina(),
                pagina.totalElementos(),
                pagina.totalPaginas()
        );
    }
}
