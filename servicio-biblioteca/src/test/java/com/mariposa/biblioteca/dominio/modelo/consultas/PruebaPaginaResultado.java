package com.mariposa.biblioteca.dominio.modelo.consultas;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PruebaPaginaResultado {

    @Nested
    class ConstructorCompacto {

        @Test
        void debeRechazarListaNula() {
            assertThatThrownBy(() -> new PaginaResultado<String>(null, 0, 10, 0L, 0))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("elementos");
        }

        @Test
        void debeCopiarListaDefensivamente() {
            var original = new ArrayList<>(List.of("a", "b", "c"));

            var resultado = new PaginaResultado<>(original, 0, 10, 3L, 1);
            original.add("d");

            assertThat(resultado.elementos()).containsExactly("a", "b", "c");
        }

        @Test
        void debeProducirListaInmutable() {
            var resultado = new PaginaResultado<>(List.of("a"), 0, 10, 1L, 1);

            assertThatThrownBy(() -> resultado.elementos().add("b"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class FactoryDe {

        @Test
        void debeCalcularTotalPaginasConDivisionExacta() {
            var paginacion = new Paginacion(0, 10);

            var resultado = PaginaResultado.de(List.of("a", "b"), paginacion, 100L);

            assertThat(resultado.totalPaginas()).isEqualTo(10);
        }

        @Test
        void debeCalcularTotalPaginasConRestoUsandoCeilDiv() {
            var paginacion = new Paginacion(0, 10);

            var resultado = PaginaResultado.de(List.of("a", "b"), paginacion, 101L);

            assertThat(resultado.totalPaginas()).isEqualTo(11);
        }

        @Test
        void debeRetornarCeroPaginasSiNoHayElementos() {
            var paginacion = new Paginacion(0, 10);

            var resultado = PaginaResultado.de(List.of(), paginacion, 0L);

            assertThat(resultado.totalPaginas()).isZero();
        }

        @Test
        void debePropagarPaginaYTamanoDesdePaginacion() {
            var paginacion = new Paginacion(2, 25);

            var resultado = PaginaResultado.de(List.of("a"), paginacion, 75L);

            assertThat(resultado.paginaActual()).isEqualTo(2);
            assertThat(resultado.tamanoPagina()).isEqualTo(25);
            assertThat(resultado.totalElementos()).isEqualTo(75L);
        }
    }

    @Nested
    class FactoryVacia {

        @Test
        void debeRetornarResultadoConListaVacia() {
            var paginacion = new Paginacion(0, 10);

            var resultado = PaginaResultado.<String>vacia(paginacion);

            assertThat(resultado.elementos()).isEmpty();
            assertThat(resultado.totalElementos()).isZero();
            assertThat(resultado.totalPaginas()).isZero();
        }

        @Test
        void debePreservarPaginaYTamanoSolicitados() {
            var paginacion = new Paginacion(4, 15);

            var resultado = PaginaResultado.<Integer>vacia(paginacion);

            assertThat(resultado.paginaActual()).isEqualTo(4);
            assertThat(resultado.tamanoPagina()).isEqualTo(15);
        }
    }
}
