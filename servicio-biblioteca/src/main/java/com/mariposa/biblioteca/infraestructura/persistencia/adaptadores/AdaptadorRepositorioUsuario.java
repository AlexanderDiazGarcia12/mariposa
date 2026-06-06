package com.mariposa.biblioteca.infraestructura.persistencia.adaptadores;

import com.mariposa.biblioteca.dominio.modelo.Usuario;
import com.mariposa.biblioteca.dominio.modelo.consultas.OrdenUsuario;
import com.mariposa.biblioteca.dominio.modelo.consultas.PaginaResultado;
import com.mariposa.biblioteca.dominio.modelo.consultas.Paginacion;
import com.mariposa.biblioteca.dominio.modelo.valor.CorreoElectronico;
import com.mariposa.biblioteca.dominio.puertos.salida.RepositorioUsuario;
import com.mariposa.biblioteca.infraestructura.persistencia.mapeadores.MapeadorUsuario;
import com.mariposa.biblioteca.infraestructura.persistencia.repositorios.RepositorioUsuarioSpringData;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class AdaptadorRepositorioUsuario implements RepositorioUsuario {

    private static final OrdenUsuario ORDEN_POR_DEFECTO = OrdenUsuario.POR_NOMBRE_USUARIO_ASC;

    private final RepositorioUsuarioSpringData repositorio;
    private final MapeadorUsuario mapeador;

    public AdaptadorRepositorioUsuario(RepositorioUsuarioSpringData repositorio, MapeadorUsuario mapeador) {
        this.repositorio = repositorio;
        this.mapeador = mapeador;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> obtenerPorId(UUID id) {
        return repositorio.findById(id).map(mapeador::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> obtenerPorNombreUsuario(String nombreUsuario) {
        return repositorio.findByNombreUsuario(nombreUsuario).map(mapeador::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> obtenerPorCorreo(CorreoElectronico correo) {
        return repositorio.findByCorreoElectronico(correo.valor()).map(mapeador::aDominio);
    }

    @Override
    @Transactional
    public Usuario guardar(Usuario usuario) {
        var entidad = mapeador.aEntidad(usuario);
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
    public PaginaResultado<Usuario> listar(Paginacion paginacion) {
        var solicitud = PageRequest.of(paginacion.pagina(), paginacion.tamano(), traducirOrden(ORDEN_POR_DEFECTO));
        var pagina = repositorio.findAll(solicitud);
        var elementos = pagina.getContent().stream().map(mapeador::aDominio).toList();
        return PaginaResultado.de(elementos, paginacion, pagina.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorNombreUsuario(String nombreUsuario) {
        return repositorio.existsByNombreUsuario(nombreUsuario);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorCorreo(CorreoElectronico correo) {
        return repositorio.existsByCorreoElectronico(correo.valor());
    }

    private Sort traducirOrden(OrdenUsuario orden) {
        return switch (orden) {
            case POR_NOMBRE_USUARIO_ASC -> Sort.by(Sort.Direction.ASC, "nombreUsuario");
            case POR_NOMBRE_USUARIO_DESC -> Sort.by(Sort.Direction.DESC, "nombreUsuario");
            case POR_FECHA_CREACION_ASC -> Sort.by(Sort.Direction.ASC, "fechaCreacion");
            case POR_FECHA_CREACION_DESC -> Sort.by(Sort.Direction.DESC, "fechaCreacion");
        };
    }
}
