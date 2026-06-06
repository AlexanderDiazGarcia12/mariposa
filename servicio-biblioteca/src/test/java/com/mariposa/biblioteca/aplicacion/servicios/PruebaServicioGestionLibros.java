package com.mariposa.biblioteca.aplicacion.servicios;

import com.mariposa.biblioteca.dominio.excepciones.LibroNoEncontrado;
import com.mariposa.biblioteca.dominio.excepciones.LibroNoEncontradoPorIsbn;
import com.mariposa.biblioteca.dominio.excepciones.LibroYaExiste;
import com.mariposa.biblioteca.dominio.modelo.Genero;
import com.mariposa.biblioteca.dominio.modelo.Libro;
import com.mariposa.biblioteca.dominio.modelo.consultas.FiltroLibros;
import com.mariposa.biblioteca.dominio.modelo.consultas.OrdenLibro;
import com.mariposa.biblioteca.dominio.modelo.consultas.PaginaResultado;
import com.mariposa.biblioteca.dominio.modelo.consultas.Paginacion;
import com.mariposa.biblioteca.dominio.modelo.valor.Isbn;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.ActualizarLibroComando;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.CrearLibroComando;
import com.mariposa.biblioteca.dominio.puertos.salida.RepositorioLibro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PruebaServicioGestionLibros {

    private static final UUID ID_LIBRO = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final Isbn ISBN = Isbn.desTexto("9781491950357");

    @Mock
    private RepositorioLibro repositorioLibro;

    @InjectMocks
    private ServicioGestionLibros servicio;

    private Libro libroExistente;

    @BeforeEach
    void inicializar() {
        var ahora = Instant.parse("2026-01-01T00:00:00Z");
        libroExistente = new Libro(
                ID_LIBRO,
                "Clean Code",
                "Robert C. Martin",
                ISBN,
                2008,
                Genero.TECNICO,
                10,
                7,
                ahora,
                ahora
        );
    }

    @Test
    @DisplayName("crear guarda un libro nuevo cuando el ISBN no existe")
    void crearGuardaLibroNuevo() {
        given(repositorioLibro.existePorIsbn(ISBN)).willReturn(false);
        given(repositorioLibro.guardar(any(Libro.class))).willAnswer(invocacion -> invocacion.getArgument(0));

        var comando = new CrearLibroComando("Clean Code", "Robert C. Martin", ISBN, 2008, Genero.TECNICO, 5);
        var creado = servicio.crear(comando);

        assertThat(creado.titulo()).isEqualTo("Clean Code");
        assertThat(creado.copiasTotales()).isEqualTo(5);
        assertThat(creado.copiasDisponibles()).isEqualTo(5);

        var captor = ArgumentCaptor.forClass(Libro.class);
        verify(repositorioLibro).guardar(captor.capture());
        assertThat(captor.getValue().isbn()).isEqualTo(ISBN);
    }

    @Test
    @DisplayName("crear lanza LibroYaExiste cuando el ISBN ya está registrado")
    void crearFallaSiIsbnExiste() {
        given(repositorioLibro.existePorIsbn(ISBN)).willReturn(true);
        var comando = new CrearLibroComando("Clean Code", "Robert C. Martin", ISBN, 2008, Genero.TECNICO, 5);

        assertThatThrownBy(() -> servicio.crear(comando)).isInstanceOf(LibroYaExiste.class);
        verify(repositorioLibro, never()).guardar(any());
    }

    @Test
    @DisplayName("actualizar delega en actualizarCatalogo del dominio y guarda")
    void actualizarDelegaEnDominio() {
        given(repositorioLibro.obtenerPorId(ID_LIBRO)).willReturn(Optional.of(libroExistente));
        given(repositorioLibro.guardar(any(Libro.class))).willAnswer(invocacion -> invocacion.getArgument(0));

        var comando = new ActualizarLibroComando(ID_LIBRO, "Nuevo titulo", "Otro autor", 2010, Genero.CIENCIA, 12);
        var actualizado = servicio.actualizar(comando);

        assertThat(actualizado.titulo()).isEqualTo("Nuevo titulo");
        assertThat(actualizado.autor()).isEqualTo("Otro autor");
        assertThat(actualizado.copiasTotales()).isEqualTo(12);
        assertThat(actualizado.copiasDisponibles()).isEqualTo(12 - (10 - 7));
    }

    @Test
    @DisplayName("actualizar lanza LibroNoEncontrado cuando el id no existe")
    void actualizarFallaSiNoExiste() {
        given(repositorioLibro.obtenerPorId(ID_LIBRO)).willReturn(Optional.empty());
        var comando = new ActualizarLibroComando(ID_LIBRO, "T", "A", 2010, Genero.CIENCIA, 5);

        assertThatThrownBy(() -> servicio.actualizar(comando)).isInstanceOf(LibroNoEncontrado.class);
        verify(repositorioLibro, never()).guardar(any());
    }

    @Test
    @DisplayName("eliminar invoca al repositorio cuando el libro existe")
    void eliminarExitoso() {
        given(repositorioLibro.obtenerPorId(ID_LIBRO)).willReturn(Optional.of(libroExistente));

        servicio.eliminar(ID_LIBRO);

        verify(repositorioLibro).eliminar(ID_LIBRO);
    }

    @Test
    @DisplayName("eliminar lanza LibroNoEncontrado cuando el libro no existe")
    void eliminarFallaSiNoExiste() {
        given(repositorioLibro.obtenerPorId(ID_LIBRO)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.eliminar(ID_LIBRO)).isInstanceOf(LibroNoEncontrado.class);
        verify(repositorioLibro, never()).eliminar(any());
    }

    @Test
    @DisplayName("obtenerPorId devuelve libro cuando existe")
    void obtenerPorIdExitoso() {
        given(repositorioLibro.obtenerPorId(ID_LIBRO)).willReturn(Optional.of(libroExistente));

        assertThat(servicio.obtenerPorId(ID_LIBRO)).isEqualTo(libroExistente);
    }

    @Test
    @DisplayName("obtenerPorId lanza LibroNoEncontrado cuando no existe")
    void obtenerPorIdFallaSiNoExiste() {
        given(repositorioLibro.obtenerPorId(ID_LIBRO)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerPorId(ID_LIBRO)).isInstanceOf(LibroNoEncontrado.class);
    }

    @Test
    @DisplayName("obtenerPorIsbn devuelve libro cuando existe")
    void obtenerPorIsbnExitoso() {
        given(repositorioLibro.obtenerPorIsbn(ISBN)).willReturn(Optional.of(libroExistente));

        assertThat(servicio.obtenerPorIsbn(ISBN)).isEqualTo(libroExistente);
    }

    @Test
    @DisplayName("obtenerPorIsbn lanza LibroNoEncontradoPorIsbn cuando no existe")
    void obtenerPorIsbnFallaSiNoExiste() {
        given(repositorioLibro.obtenerPorIsbn(ISBN)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerPorIsbn(ISBN)).isInstanceOf(LibroNoEncontradoPorIsbn.class);
    }

    @Test
    @DisplayName("listar delega al repositorio con el filtro y paginación")
    void listarDelegaAlRepositorio() {
        var filtro = new FiltroLibros(Optional.empty(), Optional.empty(), Optional.empty(), OrdenLibro.POR_TITULO_ASC);
        var paginacion = new Paginacion(0, 20);
        var paginaEsperada = PaginaResultado.de(List.of(libroExistente), paginacion, 1L);
        given(repositorioLibro.buscar(filtro, paginacion)).willReturn(paginaEsperada);

        var resultado = servicio.listar(filtro, paginacion);

        assertThat(resultado).isEqualTo(paginaEsperada);
    }
}
