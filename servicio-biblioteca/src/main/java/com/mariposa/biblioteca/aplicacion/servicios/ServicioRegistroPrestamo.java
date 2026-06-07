package com.mariposa.biblioteca.aplicacion.servicios;

import com.mariposa.biblioteca.dominio.excepciones.LibroNoEncontrado;
import com.mariposa.biblioteca.dominio.excepciones.OperacionInvalidaDominio;
import com.mariposa.biblioteca.dominio.excepciones.UsuarioNoEncontrado;
import com.mariposa.biblioteca.dominio.modelo.Libro;
import com.mariposa.biblioteca.dominio.modelo.Usuario;
import com.mariposa.biblioteca.dominio.puertos.entrada.RegistrarPrestamoCasoUso;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.PrestamoRegistrado;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.RegistrarPrestamoComando;
import com.mariposa.biblioteca.dominio.puertos.salida.ClientePrestamos;
import com.mariposa.biblioteca.dominio.puertos.salida.RepositorioLibro;
import com.mariposa.biblioteca.dominio.puertos.salida.RepositorioUsuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioRegistroPrestamo implements RegistrarPrestamoCasoUso {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioRegistroPrestamo.class);

    private final RepositorioUsuario repositorioUsuario;
    private final RepositorioLibro repositorioLibro;
    private final ClientePrestamos clientePrestamos;

    public ServicioRegistroPrestamo(
            RepositorioUsuario repositorioUsuario,
            RepositorioLibro repositorioLibro,
            ClientePrestamos clientePrestamos
    ) {
        this.repositorioUsuario = repositorioUsuario;
        this.repositorioLibro = repositorioLibro;
        this.clientePrestamos = clientePrestamos;
    }

    @Override
    @Transactional
    public PrestamoRegistrado registrar(RegistrarPrestamoComando comando) {
        validarUsuarioActivo(comando);
        var libro = obtenerLibro(comando);
        var libroReservado = libro.prestar();
        var prestamoRegistrado = clientePrestamos.registrar(comando);
        repositorioLibro.guardar(libroReservado);
        LOGGER.info("Préstamo {} registrado para usuario {} sobre libro {}",
                prestamoRegistrado.idPrestamo(), comando.idUsuario(), comando.idLibro());
        return prestamoRegistrado;
    }

    private void validarUsuarioActivo(RegistrarPrestamoComando comando) {
        var usuario = repositorioUsuario.obtenerPorId(comando.idUsuario())
                .orElseThrow(() -> new UsuarioNoEncontrado(comando.idUsuario()));
        if (!estaActivo(usuario)) {
            throw new OperacionInvalidaDominio(
                    "El usuario %s no está activo".formatted(comando.idUsuario())
            );
        }
    }

    private boolean estaActivo(Usuario usuario) {
        return usuario.estaActivo();
    }

    private Libro obtenerLibro(RegistrarPrestamoComando comando) {
        return repositorioLibro.obtenerPorId(comando.idLibro())
                .orElseThrow(() -> new LibroNoEncontrado(comando.idLibro()));
    }
}
