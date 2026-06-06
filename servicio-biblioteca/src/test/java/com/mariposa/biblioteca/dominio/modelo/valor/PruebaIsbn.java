package com.mariposa.biblioteca.dominio.modelo.valor;

import com.mariposa.biblioteca.dominio.excepciones.ValorInvalidoDominio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PruebaIsbn {

    @Nested
    class ConstructorCompacto {

        @Test
        void debeAceptarIsbn13Valido() {
            var isbn = new Isbn("9780132350884");

            assertThat(isbn.valor()).isEqualTo("9780132350884");
        }

        @Test
        void debeAceptarIsbn10Valido() {
            var isbn = new Isbn("0132350882");

            assertThat(isbn.valor()).isEqualTo("0132350882");
        }

        @Test
        void debeAceptarIsbn10ConDigitoVerificadorX() {
            var isbn = new Isbn("097522980X");

            assertThat(isbn.valor()).isEqualTo("097522980X");
        }

        @Test
        void debeNormalizarEliminandoGuiones() {
            var isbn = new Isbn("978-0-13-235088-4");

            assertThat(isbn.valor()).isEqualTo("9780132350884");
        }

        @Test
        void debeNormalizarEliminandoEspacios() {
            var isbn = new Isbn("978 0132 350884");

            assertThat(isbn.valor()).isEqualTo("9780132350884");
        }

        @Test
        void debeRechazarIsbnNulo() {
            assertThatThrownBy(() -> new Isbn(null))
                    .isInstanceOf(ValorInvalidoDominio.class)
                    .extracting("campo").isEqualTo("isbn");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "123",
                "12345678901",
                "12345678901234",
                "abcdefghij",
                "978-013235088Z",
                ""
        })
        void debeRechazarFormatosInvalidos(String valor) {
            assertThatThrownBy(() -> new Isbn(valor))
                    .isInstanceOf(ValorInvalidoDominio.class)
                    .extracting("campo").isEqualTo("isbn");
        }
    }

    @Nested
    class FactoryDesTexto {

        @Test
        void debeConstruirIsbnDesdeTexto() {
            var isbn = Isbn.desTexto("9780132350884");

            assertThat(isbn.valor()).isEqualTo("9780132350884");
        }

        @Test
        void debeNormalizarAlConstruirDesdeTexto() {
            var isbn = Isbn.desTexto("978-0-13-235088-4");

            assertThat(isbn.valor()).isEqualTo("9780132350884");
        }
    }
}
