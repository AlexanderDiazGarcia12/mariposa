package com.mariposa.biblioteca.infraestructura.persistencia.adaptadores;

import com.mariposa.biblioteca.dominio.modelo.Libro;
import com.mariposa.biblioteca.dominio.modelo.consultas.FiltroLibros;
import com.mariposa.biblioteca.dominio.modelo.consultas.OrdenLibro;
import com.mariposa.biblioteca.dominio.modelo.consultas.PaginaResultado;
import com.mariposa.biblioteca.dominio.modelo.consultas.Paginacion;
import com.mariposa.biblioteca.dominio.modelo.valor.Isbn;
import com.mariposa.biblioteca.dominio.puertos.salida.RepositorioLibro;
import com.mariposa.biblioteca.infraestructura.persistencia.entidades.EntidadLibro;
import com.mariposa.biblioteca.infraestructura.persistencia.mapeadores.MapeadorLibro;
import com.mariposa.biblioteca.infraestructura.persistencia.repositorios.RepositorioLibroSpringData;
import com.mariposa.biblioteca.infraestructura.persistencia.specifications.EspecificacionesLibro;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class AdaptadorRepositorioLibro implements RepositorioLibro {

    private final RepositorioLibroSpringData repositorio;
    private final MapeadorLibro mapeador;

    public AdaptadorRepositorioLibro(RepositorioLibroSpringData repositorio, MapeadorLibro mapeador) {
        this.repositorio = repositorio;
        this.mapeador = mapeador;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Libro> obtenerPorId(UUID id) {
        return repositorio.findById(id).map(mapeador::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Libro> obtenerPorIsbn(Isbn isbn) {
        return repositorio.findByIsbn(isbn.valor()).map(mapeador::aDominio);
    }

    @Override
    @Transactional
    public Libro guardar(Libro libro) {
        var entidad = mapeador.aEntidad(libro);
        var guardada = repositorio.save(entidad);
        return mapeador.aDominio(guardada);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {
        repositorio.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResultado<Libro> buscar(FiltroLibros filtro, Paginacion paginacion) {
        var solicitud = PageRequest.of(paginacion.pagina(), paginacion.tamano(), traducirOrden(filtro.orden()));
        var pagina = repositorio.findAll(EspecificacionesLibro.desde(filtro), solicitud);
        var elementos = pagina.getContent().stream().map(mapeador::aDominio).toList();
        return PaginaResultado.de(elementos, paginacion, pagina.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorIsbn(Isbn isbn) {
        return repositorio.existsByIsbn(isbn.valor());
    }

    private Sort traducirOrden(OrdenLibro orden) {
        return switch (orden) {
            case POR_TITULO_ASC -> Sort.by(Sort.Direction.ASC, "titulo");
            case POR_TITULO_DESC -> Sort.by(Sort.Direction.DESC, "titulo");
            case POR_AUTOR_ASC -> Sort.by(Sort.Direction.ASC, "autor");
            case POR_AUTOR_DESC -> Sort.by(Sort.Direction.DESC, "autor");
            case POR_FECHA_CREACION_ASC -> Sort.by(Sort.Direction.ASC, "fechaCreacion");
            case POR_FECHA_CREACION_DESC -> Sort.by(Sort.Direction.DESC, "fechaCreacion");
        };
    }
}
