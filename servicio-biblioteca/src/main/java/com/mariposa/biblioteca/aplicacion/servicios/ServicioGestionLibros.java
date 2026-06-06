package com.mariposa.biblioteca.aplicacion.servicios;

import com.mariposa.biblioteca.dominio.excepciones.LibroNoEncontrado;
import com.mariposa.biblioteca.dominio.excepciones.LibroNoEncontradoPorIsbn;
import com.mariposa.biblioteca.dominio.excepciones.LibroYaExiste;
import com.mariposa.biblioteca.dominio.modelo.Libro;
import com.mariposa.biblioteca.dominio.modelo.consultas.FiltroLibros;
import com.mariposa.biblioteca.dominio.modelo.consultas.PaginaResultado;
import com.mariposa.biblioteca.dominio.modelo.consultas.Paginacion;
import com.mariposa.biblioteca.dominio.modelo.valor.Isbn;
import com.mariposa.biblioteca.dominio.puertos.entrada.GestionarLibrosCasoUso;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.ActualizarLibroComando;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.CrearLibroComando;
import com.mariposa.biblioteca.dominio.puertos.salida.RepositorioLibro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ServicioGestionLibros implements GestionarLibrosCasoUso {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioGestionLibros.class);

    private final RepositorioLibro repositorioLibro;

    public ServicioGestionLibros(RepositorioLibro repositorioLibro) {
        this.repositorioLibro = repositorioLibro;
    }

    @Override
    @Transactional
    public Libro crear(CrearLibroComando comando) {
        if (repositorioLibro.existePorIsbn(comando.isbn())) {
            throw new LibroYaExiste(comando.isbn());
        }
        var nuevoLibro = Libro.nuevo(
                UUID.randomUUID(),
                comando.titulo(),
                comando.autor(),
                comando.isbn(),
                comando.anioPublicacion(),
                comando.genero(),
                comando.copiasTotales()
        );
        var guardado = repositorioLibro.guardar(nuevoLibro);
        LOGGER.info("Libro creado con id {} e ISBN {}", guardado.id(), guardado.isbn().valor());
        return guardado;
    }

    @Override
    @Transactional
    public Libro actualizar(ActualizarLibroComando comando) {
        var libroExistente = repositorioLibro.obtenerPorId(comando.id())
                .orElseThrow(() -> new LibroNoEncontrado(comando.id()));
        var libroActualizado = libroExistente.actualizarCatalogo(
                comando.titulo(),
                comando.autor(),
                comando.anioPublicacion(),
                comando.genero(),
                comando.copiasTotales()
        );
        var guardado = repositorioLibro.guardar(libroActualizado);
        LOGGER.info("Libro actualizado con id {}", guardado.id());
        return guardado;
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {
        var libroExistente = repositorioLibro.obtenerPorId(id)
                .orElseThrow(() -> new LibroNoEncontrado(id));
        repositorioLibro.eliminar(libroExistente.id());
        LOGGER.info("Libro eliminado con id {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Libro obtenerPorId(UUID id) {
        return repositorioLibro.obtenerPorId(id)
                .orElseThrow(() -> new LibroNoEncontrado(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Libro obtenerPorIsbn(Isbn isbn) {
        return repositorioLibro.obtenerPorIsbn(isbn)
                .orElseThrow(() -> new LibroNoEncontradoPorIsbn(isbn));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResultado<Libro> listar(FiltroLibros filtro, Paginacion paginacion) {
        return repositorioLibro.buscar(filtro, paginacion);
    }
}
