package com.mariposa.biblioteca.aplicacion.servicios;

import com.mariposa.biblioteca.dominio.excepciones.LibroNoEncontrado;
import com.mariposa.biblioteca.dominio.excepciones.OperacionInvalidaDominio;
import com.mariposa.biblioteca.dominio.excepciones.PrestamoRechazadoPorServicioB;
import com.mariposa.biblioteca.dominio.excepciones.ServicioPrestamosNoDisponible;
import com.mariposa.biblioteca.dominio.excepciones.SinCopiasDisponibles;
import com.mariposa.biblioteca.dominio.excepciones.UsuarioNoEncontrado;
import com.mariposa.biblioteca.dominio.modelo.EstadoUsuario;
import com.mariposa.biblioteca.dominio.modelo.Genero;
import com.mariposa.biblioteca.dominio.modelo.Libro;
import com.mariposa.biblioteca.dominio.modelo.Rol;
import com.mariposa.biblioteca.dominio.modelo.Usuario;
import com.mariposa.biblioteca.dominio.modelo.valor.CorreoElectronico;
import com.mariposa.biblioteca.dominio.modelo.valor.Isbn;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.PrestamoRegistrado;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.RegistrarPrestamoComando;
import com.mariposa.biblioteca.dominio.puertos.salida.ClientePrestamos;
import com.mariposa.biblioteca.dominio.puertos.salida.RepositorioLibro;
import com.mariposa.biblioteca.dominio.puertos.salida.RepositorioUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PruebaServicioRegistroPrestamo {

    private static final UUID ID_USUARIO = UUID.fromString("aaaaaaaa-1111-2222-3333-cccccccccccc");
    private static final UUID ID_LIBRO = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID ID_PRESTAMO = UUID.fromString("99999999-aaaa-bbbb-cccc-dddddddddddd");
    private static final LocalDate FECHA_PRESTAMO = LocalDate.parse("2026-06-06");
    private static final LocalDate FECHA_DEVOLUCION = LocalDate.parse("2026-06-20");
    private static final Isbn ISBN = Isbn.desTexto("9781491950357");

    @Mock
    private RepositorioUsuario repositorioUsuario;

    @Mock
    private RepositorioLibro repositorioLibro;

    @Mock
    private ClientePrestamos clientePrestamos;

    @InjectMocks
    private ServicioRegistroPrestamo servicio;

    private Usuario usuarioActivo;
    private Usuario usuarioInactivo;
    private Libro libroConCopias;
    private Libro libroSinCopias;
    private RegistrarPrestamoComando comando;

    @BeforeEach
    void inicializar() {
        var ahora = Instant.parse("2026-06-01T00:00:00Z");
        usuarioActivo = new Usuario(
                ID_USUARIO, "usuario-prueba", new CorreoElectronico("correo@correo.com"),
                "hash", Rol.USUARIO, EstadoUsuario.ACTIVO, ahora, ahora
        );
        usuarioInactivo = new Usuario(
                ID_USUARIO, "usuario-prueba", new CorreoElectronico("correo@correo.com"),
                "hash", Rol.USUARIO, EstadoUsuario.INACTIVO, ahora, ahora
        );
        libroConCopias = new Libro(
                ID_LIBRO, "Clean Code", "Robert C. Martin", ISBN, 2008, Genero.TECNICO,
                5, 3, ahora, ahora
        );
        libroSinCopias = new Libro(
                ID_LIBRO, "Clean Code", "Robert C. Martin", ISBN, 2008, Genero.TECNICO,
                5, 0, ahora, ahora
        );
        comando = new RegistrarPrestamoComando(ID_USUARIO, ID_LIBRO, FECHA_PRESTAMO, FECHA_DEVOLUCION);
    }

    @Test
    @DisplayName("registrar decrementa copias y devuelve PrestamoRegistrado en el camino feliz")
    void registrarCaminoFeliz() {
        var prestamoEsperado = new PrestamoRegistrado(
                ID_PRESTAMO, ID_USUARIO, ID_LIBRO, FECHA_PRESTAMO, FECHA_DEVOLUCION,
                "ACTIVO", Instant.parse("2026-06-06T10:00:00Z")
        );
        given(repositorioUsuario.obtenerPorId(ID_USUARIO)).willReturn(Optional.of(usuarioActivo));
        given(repositorioLibro.obtenerPorId(ID_LIBRO)).willReturn(Optional.of(libroConCopias));
        given(clientePrestamos.registrar(comando)).willReturn(prestamoEsperado);
        given(repositorioLibro.guardar(any(Libro.class))).willAnswer(inv -> inv.getArgument(0));

        var resultado = servicio.registrar(comando);

        assertThat(resultado).isEqualTo(prestamoEsperado);
        var captor = ArgumentCaptor.forClass(Libro.class);
        verify(repositorioLibro).guardar(captor.capture());
        assertThat(captor.getValue().copiasDisponibles()).isEqualTo(2);
    }

    @Test
    @DisplayName("registrar lanza UsuarioNoEncontrado cuando el usuario no existe")
    void registrarFallaSiUsuarioNoExiste() {
        given(repositorioUsuario.obtenerPorId(ID_USUARIO)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.registrar(comando))
                .isInstanceOf(UsuarioNoEncontrado.class);
        verify(clientePrestamos, never()).registrar(any());
        verify(repositorioLibro, never()).guardar(any());
    }

    @Test
    @DisplayName("registrar lanza OperacionInvalidaDominio cuando el usuario no está activo")
    void registrarFallaSiUsuarioInactivo() {
        given(repositorioUsuario.obtenerPorId(ID_USUARIO)).willReturn(Optional.of(usuarioInactivo));

        assertThatThrownBy(() -> servicio.registrar(comando))
                .isInstanceOf(OperacionInvalidaDominio.class)
                .hasMessageContaining("no está activo");
        verify(clientePrestamos, never()).registrar(any());
    }

    @Test
    @DisplayName("registrar lanza LibroNoEncontrado cuando el libro no existe")
    void registrarFallaSiLibroNoExiste() {
        given(repositorioUsuario.obtenerPorId(ID_USUARIO)).willReturn(Optional.of(usuarioActivo));
        given(repositorioLibro.obtenerPorId(ID_LIBRO)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.registrar(comando))
                .isInstanceOf(LibroNoEncontrado.class);
        verify(clientePrestamos, never()).registrar(any());
    }

    @Test
    @DisplayName("registrar lanza SinCopiasDisponibles cuando el libro no tiene copias")
    void registrarFallaSiSinCopias() {
        given(repositorioUsuario.obtenerPorId(ID_USUARIO)).willReturn(Optional.of(usuarioActivo));
        given(repositorioLibro.obtenerPorId(ID_LIBRO)).willReturn(Optional.of(libroSinCopias));

        assertThatThrownBy(() -> servicio.registrar(comando))
                .isInstanceOf(SinCopiasDisponibles.class);
        verify(clientePrestamos, never()).registrar(any());
        verify(repositorioLibro, never()).guardar(any());
    }

    @Test
    @DisplayName("registrar propaga ServicioPrestamosNoDisponible sin decrementar copias")
    void registrarPropagaServicioPrestamosNoDisponible() {
        given(repositorioUsuario.obtenerPorId(ID_USUARIO)).willReturn(Optional.of(usuarioActivo));
        given(repositorioLibro.obtenerPorId(ID_LIBRO)).willReturn(Optional.of(libroConCopias));
        given(clientePrestamos.registrar(comando))
                .willThrow(new ServicioPrestamosNoDisponible("Circuit abierto"));

        assertThatThrownBy(() -> servicio.registrar(comando))
                .isInstanceOf(ServicioPrestamosNoDisponible.class);
        verify(repositorioLibro, never()).guardar(any());
    }

    @Test
    @DisplayName("registrar propaga PrestamoRechazadoPorServicioB sin decrementar copias")
    void registrarPropagaPrestamoRechazado() {
        given(repositorioUsuario.obtenerPorId(ID_USUARIO)).willReturn(Optional.of(usuarioActivo));
        given(repositorioLibro.obtenerPorId(ID_LIBRO)).willReturn(Optional.of(libroConCopias));
        given(clientePrestamos.registrar(comando))
                .willThrow(new PrestamoRechazadoPorServicioB("422", "Usuario con préstamos vencidos"));

        assertThatThrownBy(() -> servicio.registrar(comando))
                .isInstanceOf(PrestamoRechazadoPorServicioB.class);
        verify(repositorioLibro, never()).guardar(any());
    }
}
