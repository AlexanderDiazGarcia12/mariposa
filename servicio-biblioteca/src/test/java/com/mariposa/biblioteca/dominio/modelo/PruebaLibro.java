package com.mariposa.biblioteca.dominio.modelo;

import com.mariposa.biblioteca.dominio.excepciones.OperacionInvalidaDominio;
import com.mariposa.biblioteca.dominio.excepciones.SinCopiasDisponibles;
import com.mariposa.biblioteca.dominio.excepciones.ValorInvalidoDominio;
import com.mariposa.biblioteca.dominio.modelo.valor.Isbn;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PruebaLibro {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Isbn ISBN = Isbn.desTexto("9780132350884");
    private static final int ANIO_VALIDO = 2020;
    private static final Instant AHORA = Instant.parse("2026-01-15T10:00:00Z");

    private static Libro libroValido(int copiasTotales, int copiasDisponibles) {
        return new Libro(
                ID, "Clean Code", "Robert C. Martin", ISBN, ANIO_VALIDO, Genero.TECNICO,
                copiasTotales, copiasDisponibles, AHORA, AHORA
        );
    }

    @Nested
    class ConstructorCompacto {

        @Test
        void debeRechazarIdNulo() {
            assertThatThrownBy(() -> new Libro(
                    null, "Titulo", "Autor", ISBN, ANIO_VALIDO, Genero.FICCION,
                    3, 3, AHORA, AHORA
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("id");
        }

        @Test
        void debeRechazarIsbnNulo() {
            assertThatThrownBy(() -> new Libro(
                    ID, "Titulo", "Autor", null, ANIO_VALIDO, Genero.FICCION,
                    3, 3, AHORA, AHORA
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("isbn");
        }

        @Test
        void debeRechazarGeneroNulo() {
            assertThatThrownBy(() -> new Libro(
                    ID, "Titulo", "Autor", ISBN, ANIO_VALIDO, null,
                    3, 3, AHORA, AHORA
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("genero");
        }

        @Test
        void debeRechazarFechaCreacionNula() {
            assertThatThrownBy(() -> new Libro(
                    ID, "Titulo", "Autor", ISBN, ANIO_VALIDO, Genero.FICCION,
                    3, 3, null, AHORA
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("fechaCreacion");
        }

        @Test
        void debeRechazarFechaActualizacionNula() {
            assertThatThrownBy(() -> new Libro(
                    ID, "Titulo", "Autor", ISBN, ANIO_VALIDO, Genero.FICCION,
                    3, 3, AHORA, null
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("fechaActualizacion");
        }

        @Test
        void debeRechazarTituloEnBlanco() {
            assertThatThrownBy(() -> new Libro(
                    ID, "   ", "Autor", ISBN, ANIO_VALIDO, Genero.FICCION,
                    3, 3, AHORA, AHORA
            ))
                    .isInstanceOf(ValorInvalidoDominio.class)
                    .extracting("campo").isEqualTo("titulo");
        }

        @Test
        void debeRechazarTituloNulo() {
            assertThatThrownBy(() -> new Libro(
                    ID, null, "Autor", ISBN, ANIO_VALIDO, Genero.FICCION,
                    3, 3, AHORA, AHORA
            ))
                    .isInstanceOf(ValorInvalidoDominio.class)
                    .extracting("campo").isEqualTo("titulo");
        }

        @Test
        void debeRechazarAutorEnBlanco() {
            assertThatThrownBy(() -> new Libro(
                    ID, "Titulo", "  ", ISBN, ANIO_VALIDO, Genero.FICCION,
                    3, 3, AHORA, AHORA
            ))
                    .isInstanceOf(ValorInvalidoDominio.class)
                    .extracting("campo").isEqualTo("autor");
        }

        @Test
        void debeRechazarTituloDemasiadoLargo() {
            var tituloLargo = "x".repeat(256);

            assertThatThrownBy(() -> new Libro(
                    ID, tituloLargo, "Autor", ISBN, ANIO_VALIDO, Genero.FICCION,
                    3, 3, AHORA, AHORA
            ))
                    .isInstanceOf(ValorInvalidoDominio.class)
                    .hasMessageContaining("longitud máxima");
        }

        @Test
        void debeRechazarAnioMenorAlMinimo() {
            assertThatThrownBy(() -> new Libro(
                    ID, "Titulo", "Autor", ISBN, 999, Genero.FICCION,
                    3, 3, AHORA, AHORA
            ))
                    .isInstanceOf(ValorInvalidoDominio.class)
                    .extracting("campo").isEqualTo("anioPublicacion");
        }

        @Test
        void debeRechazarAnioFuturo() {
            var anioFuturo = LocalDate.now().getYear() + 1;

            assertThatThrownBy(() -> new Libro(
                    ID, "Titulo", "Autor", ISBN, anioFuturo, Genero.FICCION,
                    3, 3, AHORA, AHORA
            ))
                    .isInstanceOf(ValorInvalidoDominio.class)
                    .extracting("campo").isEqualTo("anioPublicacion");
        }

        @Test
        void debeRechazarCopiasTotalesNegativas() {
            assertThatThrownBy(() -> new Libro(
                    ID, "Titulo", "Autor", ISBN, ANIO_VALIDO, Genero.FICCION,
                    -1, 0, AHORA, AHORA
            ))
                    .isInstanceOf(ValorInvalidoDominio.class)
                    .extracting("campo").isEqualTo("copiasTotales");
        }

        @Test
        void debeRechazarCopiasDisponiblesNegativas() {
            assertThatThrownBy(() -> new Libro(
                    ID, "Titulo", "Autor", ISBN, ANIO_VALIDO, Genero.FICCION,
                    3, -1, AHORA, AHORA
            ))
                    .isInstanceOf(ValorInvalidoDominio.class)
                    .extracting("campo").isEqualTo("copiasDisponibles");
        }

        @Test
        void debeRechazarCopiasDisponiblesMayoresQueTotales() {
            assertThatThrownBy(() -> new Libro(
                    ID, "Titulo", "Autor", ISBN, ANIO_VALIDO, Genero.FICCION,
                    3, 5, AHORA, AHORA
            ))
                    .isInstanceOf(ValorInvalidoDominio.class)
                    .extracting("campo").isEqualTo("copiasDisponibles");
        }

        @Test
        void debeAceptarParametrosValidos() {
            var libro = libroValido(5, 3);

            assertThat(libro.id()).isEqualTo(ID);
            assertThat(libro.copiasTotales()).isEqualTo(5);
            assertThat(libro.copiasDisponibles()).isEqualTo(3);
        }
    }

    @Nested
    class FactoryNuevo {

        @Test
        void debeCrearLibroConCopiasDisponiblesIgualesACopiasTotales() {
            var libro = Libro.nuevo(ID, "Titulo", "Autor", ISBN, ANIO_VALIDO, Genero.HISTORIA, 7);

            assertThat(libro.copiasDisponibles()).isEqualTo(libro.copiasTotales()).isEqualTo(7);
        }

        @Test
        void debeCrearLibroConFechaCreacionIgualAFechaActualizacion() {
            var libro = Libro.nuevo(ID, "Titulo", "Autor", ISBN, ANIO_VALIDO, Genero.CIENCIA, 1);

            assertThat(libro.fechaCreacion()).isEqualTo(libro.fechaActualizacion());
        }

        @Test
        void debeCrearLibroConCamposCoherentes() {
            var libro = Libro.nuevo(ID, "Titulo", "Autor", ISBN, ANIO_VALIDO, Genero.FICCION, 4);

            assertThat(libro.id()).isEqualTo(ID);
            assertThat(libro.titulo()).isEqualTo("Titulo");
            assertThat(libro.autor()).isEqualTo("Autor");
            assertThat(libro.isbn()).isEqualTo(ISBN);
            assertThat(libro.anioPublicacion()).isEqualTo(ANIO_VALIDO);
            assertThat(libro.genero()).isEqualTo(Genero.FICCION);
        }
    }

    @Nested
    class Prestar {

        @Test
        void debeDecrementarCopiasDisponiblesAlPrestar() {
            var libro = libroValido(5, 3);

            var prestado = libro.prestar();

            assertThat(prestado.copiasDisponibles()).isEqualTo(2);
            assertThat(prestado.copiasTotales()).isEqualTo(5);
        }

        @Test
        void debeLanzarSinCopiasDisponiblesAlPrestarLibroSinCopias() {
            var libro = libroValido(2, 0);

            assertThatThrownBy(libro::prestar)
                    .isInstanceOf(SinCopiasDisponibles.class)
                    .hasMessageContaining(ID.toString());
        }

        @Test
        void debeActualizarFechaActualizacionAlPrestar() {
            var libro = libroValido(3, 3);

            var prestado = libro.prestar();

            assertThat(prestado.fechaActualizacion()).isAfterOrEqualTo(libro.fechaActualizacion());
            assertThat(prestado.fechaCreacion()).isEqualTo(libro.fechaCreacion());
        }

        @Test
        void debeAdjuntarIdDeLibroEnSinCopiasDisponibles() {
            var libro = libroValido(1, 0);

            assertThatThrownBy(libro::prestar)
                    .isInstanceOfSatisfying(SinCopiasDisponibles.class,
                            ex -> assertThat(ex.idLibro()).isEqualTo(ID));
        }
    }

    @Nested
    class Devolver {

        @Test
        void debeIncrementarCopiasDisponiblesAlDevolver() {
            var libro = libroValido(5, 2);

            var devuelto = libro.devolver();

            assertThat(devuelto.copiasDisponibles()).isEqualTo(3);
            assertThat(devuelto.copiasTotales()).isEqualTo(5);
        }

        @Test
        void debeLanzarOperacionInvalidaAlDevolverMasAllaDeTotales() {
            var libro = libroValido(3, 3);

            assertThatThrownBy(libro::devolver)
                    .isInstanceOf(OperacionInvalidaDominio.class)
                    .hasMessageContaining(ID.toString());
        }

        @Test
        void debeActualizarFechaActualizacionAlDevolver() {
            var libro = libroValido(3, 1);

            var devuelto = libro.devolver();

            assertThat(devuelto.fechaActualizacion()).isAfterOrEqualTo(libro.fechaActualizacion());
            assertThat(devuelto.fechaCreacion()).isEqualTo(libro.fechaCreacion());
        }
    }

    @Nested
    class ActualizarCatalogo {

        @Test
        void debeActualizarCamposPreservandoIdYIsbn() {
            var libro = libroValido(5, 5);

            var actualizado = libro.actualizarCatalogo(
                    "Nuevo Titulo", "Nuevo Autor", 2010, Genero.BIOGRAFIA, 5
            );

            assertThat(actualizado.id()).isEqualTo(libro.id());
            assertThat(actualizado.isbn()).isEqualTo(libro.isbn());
            assertThat(actualizado.titulo()).isEqualTo("Nuevo Titulo");
            assertThat(actualizado.autor()).isEqualTo("Nuevo Autor");
            assertThat(actualizado.anioPublicacion()).isEqualTo(2010);
            assertThat(actualizado.genero()).isEqualTo(Genero.BIOGRAFIA);
        }

        @Test
        void debeRecalcularCopiasDisponiblesAlBajarTotales() {
            var libro = libroValido(10, 7);

            var actualizado = libro.actualizarCatalogo(
                    "Titulo", "Autor", ANIO_VALIDO, Genero.FICCION, 8
            );

            assertThat(actualizado.copiasTotales()).isEqualTo(8);
            assertThat(actualizado.copiasDisponibles()).isEqualTo(5);
        }

        @Test
        void debeMantenerCopiasEnPrestamoAlSubirTotales() {
            var libro = libroValido(5, 2);

            var actualizado = libro.actualizarCatalogo(
                    "Titulo", "Autor", ANIO_VALIDO, Genero.FICCION, 10
            );

            assertThat(actualizado.copiasTotales()).isEqualTo(10);
            assertThat(actualizado.copiasDisponibles()).isEqualTo(7);
        }

        @Test
        void debeLanzarOperacionInvalidaCuandoNuevasTotalesSonMenoresQueCopiasPrestadas() {
            var libro = libroValido(10, 3);

            assertThatThrownBy(() -> libro.actualizarCatalogo(
                    "Titulo", "Autor", ANIO_VALIDO, Genero.FICCION, 5
            ))
                    .isInstanceOf(OperacionInvalidaDominio.class)
                    .hasMessageContaining("5")
                    .hasMessageContaining("7");
        }

        @Test
        void debeActualizarFechaActualizacionPreservandoFechaCreacion() {
            var libro = libroValido(3, 3);

            var actualizado = libro.actualizarCatalogo(
                    "Titulo", "Autor", ANIO_VALIDO, Genero.FICCION, 3
            );

            assertThat(actualizado.fechaCreacion()).isEqualTo(libro.fechaCreacion());
            assertThat(actualizado.fechaActualizacion()).isAfterOrEqualTo(libro.fechaActualizacion());
        }
    }
}
