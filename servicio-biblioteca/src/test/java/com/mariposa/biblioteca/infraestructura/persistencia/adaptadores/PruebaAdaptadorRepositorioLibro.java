package com.mariposa.biblioteca.infraestructura.persistencia.adaptadores;

import com.mariposa.biblioteca.dominio.modelo.Genero;
import com.mariposa.biblioteca.dominio.modelo.Libro;
import com.mariposa.biblioteca.dominio.modelo.consultas.FiltroLibros;
import com.mariposa.biblioteca.dominio.modelo.consultas.OrdenLibro;
import com.mariposa.biblioteca.dominio.modelo.consultas.Paginacion;
import com.mariposa.biblioteca.dominio.modelo.valor.Isbn;
import com.mariposa.biblioteca.infraestructura.persistencia.BasePruebaPersistencia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PruebaAdaptadorRepositorioLibro extends BasePruebaPersistencia {

    private static final String ISBN_CIEN_ANIOS = "9780307474728";
    private static final String ISBN_RAYUELA = "9788437604572";
    private static final String ISBN_QUIJOTE = "9788491050292";
    private static final String ISBN_BREVE_HISTORIA = "9780553380163";

    @Autowired
    private AdaptadorRepositorioLibro adaptador;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void limpiarBaseDatos() {
        entityManager.getEntityManager().createQuery("delete from EntidadLibro").executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("guardar persiste el libro y obtenerPorId lo recupera con todos los campos")
    void guardarYObtenerPorId() {
        var libro = nuevoLibro("Cien anios de soledad", "Gabriel Garcia Marquez", ISBN_CIEN_ANIOS, 1967, Genero.FICCION, 5);

        var guardado = adaptador.guardar(libro);
        flushYLimpiar();

        var recuperado = adaptador.obtenerPorId(guardado.id());
        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().titulo()).isEqualTo("Cien anios de soledad");
        assertThat(recuperado.get().autor()).isEqualTo("Gabriel Garcia Marquez");
        assertThat(recuperado.get().isbn().valor()).isEqualTo(ISBN_CIEN_ANIOS);
        assertThat(recuperado.get().genero()).isEqualTo(Genero.FICCION);
        assertThat(recuperado.get().copiasTotales()).isEqualTo(5);
        assertThat(recuperado.get().copiasDisponibles()).isEqualTo(5);
    }

    @Test
    @DisplayName("obtenerPorIsbn recupera un libro existente por su ISBN")
    void obtenerPorIsbn() {
        var libro = nuevoLibro("Rayuela", "Julio Cortazar", ISBN_RAYUELA, 1963, Genero.FICCION, 3);
        adaptador.guardar(libro);
        flushYLimpiar();

        var recuperado = adaptador.obtenerPorIsbn(new Isbn(ISBN_RAYUELA));

        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().titulo()).isEqualTo("Rayuela");
    }

    @Test
    @DisplayName("obtenerPorIsbn retorna vacio cuando no existe el libro")
    void obtenerPorIsbnInexistente() {
        var recuperado = adaptador.obtenerPorIsbn(new Isbn(ISBN_CIEN_ANIOS));

        assertThat(recuperado).isEmpty();
    }

    @Test
    @DisplayName("existePorIsbn devuelve true cuando el libro existe y false en caso contrario")
    void existePorIsbn() {
        var libro = nuevoLibro("El Quijote", "Miguel de Cervantes", ISBN_QUIJOTE, 1605, Genero.FICCION, 4);
        adaptador.guardar(libro);
        flushYLimpiar();

        assertThat(adaptador.existePorIsbn(new Isbn(ISBN_QUIJOTE))).isTrue();
        assertThat(adaptador.existePorIsbn(new Isbn(ISBN_RAYUELA))).isFalse();
    }

    @Test
    @DisplayName("eliminar borra el libro por id")
    void eliminarPorId() {
        var libro = nuevoLibro("Breve historia del tiempo", "Stephen Hawking", ISBN_BREVE_HISTORIA, 1988, Genero.CIENCIA, 2);
        var guardado = adaptador.guardar(libro);
        flushYLimpiar();

        adaptador.eliminar(guardado.id());
        flushYLimpiar();

        assertThat(adaptador.obtenerPorId(guardado.id())).isEmpty();
    }

    @Test
    @DisplayName("buscar sin filtros devuelve la pagina con todos los libros")
    void buscarSinFiltros() {
        guardarVariosLibros();

        var resultado = adaptador.buscar(FiltroLibros.sinFiltros(), new Paginacion(0, 10));

        assertThat(resultado.elementos()).hasSize(4);
        assertThat(resultado.totalElementos()).isEqualTo(4);
        assertThat(resultado.paginaActual()).isZero();
        assertThat(resultado.tamanoPagina()).isEqualTo(10);
    }

    @Test
    @DisplayName("buscar con filtro por autor case-insensitive devuelve solo los libros del autor")
    void buscarPorAutorCaseInsensitive() {
        guardarVariosLibros();

        var filtro = new FiltroLibros(
                Optional.of("gabriel"),
                Optional.empty(),
                Optional.empty(),
                OrdenLibro.POR_TITULO_ASC
        );

        var resultado = adaptador.buscar(filtro, new Paginacion(0, 10));

        assertThat(resultado.elementos()).hasSize(1);
        assertThat(resultado.elementos().getFirst().autor()).isEqualTo("Gabriel Garcia Marquez");
    }

    @Test
    @DisplayName("buscar con filtro por genero devuelve solo los libros de ese genero")
    void buscarPorGenero() {
        guardarVariosLibros();

        var filtro = new FiltroLibros(
                Optional.empty(),
                Optional.of(Genero.CIENCIA),
                Optional.empty(),
                OrdenLibro.POR_TITULO_ASC
        );

        var resultado = adaptador.buscar(filtro, new Paginacion(0, 10));

        assertThat(resultado.elementos()).hasSize(1);
        assertThat(resultado.elementos().getFirst().titulo()).isEqualTo("Breve historia del tiempo");
    }

    @Test
    @DisplayName("buscar con filtro de disponibilidad devuelve solo libros con copias disponibles > 0")
    void buscarConDisponibilidad() {
        guardarVariosLibros();
        var quijote = adaptador.obtenerPorIsbn(new Isbn(ISBN_QUIJOTE)).orElseThrow();
        var sinCopias = new Libro(
                quijote.id(), quijote.titulo(), quijote.autor(), quijote.isbn(),
                quijote.anioPublicacion(), quijote.genero(),
                quijote.copiasTotales(), 0,
                quijote.fechaCreacion(), quijote.fechaActualizacion()
        );
        adaptador.guardar(sinCopias);
        flushYLimpiar();

        var filtro = new FiltroLibros(
                Optional.empty(),
                Optional.empty(),
                Optional.of(true),
                OrdenLibro.POR_TITULO_ASC
        );

        var resultado = adaptador.buscar(filtro, new Paginacion(0, 10));

        assertThat(resultado.elementos()).hasSize(3);
        assertThat(resultado.elementos())
                .extracting(libro -> libro.isbn().valor())
                .doesNotContain(ISBN_QUIJOTE);
    }

    @Test
    @DisplayName("buscar con filtros combinados aplica todos los criterios")
    void buscarConFiltrosCombinados() {
        guardarVariosLibros();

        var filtro = new FiltroLibros(
                Optional.of("julio"),
                Optional.of(Genero.FICCION),
                Optional.of(true),
                OrdenLibro.POR_TITULO_ASC
        );

        var resultado = adaptador.buscar(filtro, new Paginacion(0, 10));

        assertThat(resultado.elementos()).hasSize(1);
        assertThat(resultado.elementos().getFirst().titulo()).isEqualTo("Rayuela");
    }

    @Test
    @DisplayName("buscar ordenado por titulo ascendente devuelve los libros en orden alfabetico")
    void buscarOrdenadoPorTituloAscendente() {
        guardarVariosLibros();

        var filtro = new FiltroLibros(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                OrdenLibro.POR_TITULO_ASC
        );

        var resultado = adaptador.buscar(filtro, new Paginacion(0, 10));

        assertThat(resultado.elementos())
                .extracting(Libro::titulo)
                .containsExactly(
                        "Breve historia del tiempo",
                        "Cien anios de soledad",
                        "El Quijote",
                        "Rayuela"
                );
    }

    @Test
    @DisplayName("buscar ordenado por autor descendente invierte el orden alfabetico")
    void buscarOrdenadoPorAutorDescendente() {
        guardarVariosLibros();

        var filtro = new FiltroLibros(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                OrdenLibro.POR_AUTOR_DESC
        );

        var resultado = adaptador.buscar(filtro, new Paginacion(0, 10));

        assertThat(resultado.elementos())
                .extracting(Libro::autor)
                .containsExactly(
                        "Stephen Hawking",
                        "Miguel de Cervantes",
                        "Julio Cortazar",
                        "Gabriel Garcia Marquez"
                );
    }

    @Test
    @DisplayName("buscar paginado limita los resultados por pagina")
    void buscarPaginado() {
        guardarVariosLibros();

        var resultado = adaptador.buscar(FiltroLibros.sinFiltros(), new Paginacion(0, 2));

        assertThat(resultado.elementos()).hasSize(2);
        assertThat(resultado.totalElementos()).isEqualTo(4);
        assertThat(resultado.totalPaginas()).isEqualTo(2);
    }

    private void guardarVariosLibros() {
        adaptador.guardar(nuevoLibro("Cien anios de soledad", "Gabriel Garcia Marquez", ISBN_CIEN_ANIOS, 1967, Genero.FICCION, 5));
        adaptador.guardar(nuevoLibro("Rayuela", "Julio Cortazar", ISBN_RAYUELA, 1963, Genero.FICCION, 3));
        adaptador.guardar(nuevoLibro("El Quijote", "Miguel de Cervantes", ISBN_QUIJOTE, 1605, Genero.FICCION, 4));
        adaptador.guardar(nuevoLibro("Breve historia del tiempo", "Stephen Hawking", ISBN_BREVE_HISTORIA, 1988, Genero.CIENCIA, 2));
        flushYLimpiar();
    }

    private Libro nuevoLibro(String titulo, String autor, String isbn, int anio, Genero genero, int copias) {
        return Libro.nuevo(UUID.randomUUID(), titulo, autor, new Isbn(isbn), anio, genero, copias);
    }

    private void flushYLimpiar() {
        entityManager.flush();
        entityManager.clear();
    }
}
