package com.mariposa.biblioteca.dominio.modelo.consultas;

import com.mariposa.biblioteca.dominio.excepciones.ValorInvalidoDominio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PruebaPaginacion {

    @Nested
    class ConstructorCompacto {

        @Test
        void debeAceptarPaginaCeroYTamanoPositivo() {
            var paginacion = new Paginacion(0, 20);

            assertThat(paginacion.pagina()).isZero();
            assertThat(paginacion.tamano()).isEqualTo(20);
        }

        @Test
        void debeAceptarPaginaPositiva() {
            var paginacion = new Paginacion(5, 50);

            assertThat(paginacion.pagina()).isEqualTo(5);
            assertThat(paginacion.tamano()).isEqualTo(50);
        }

        @Test
        void debeRechazarPaginaNegativa() {
            assertThatThrownBy(() -> new Paginacion(-1, 20))
                    .isInstanceOf(ValorInvalidoDominio.class)
                    .extracting("campo").isEqualTo("pagina");
        }

        @Test
        void debeRechazarTamanoCero() {
            assertThatThrownBy(() -> new Paginacion(0, 0))
                    .isInstanceOf(ValorInvalidoDominio.class)
                    .extracting("campo").isEqualTo("tamano");
        }

        @Test
        void debeRechazarTamanoNegativo() {
            assertThatThrownBy(() -> new Paginacion(0, -5))
                    .isInstanceOf(ValorInvalidoDominio.class)
                    .extracting("campo").isEqualTo("tamano");
        }

        @Test
        void debeRechazarTamanoMayorAlMaximo() {
            assertThatThrownBy(() -> new Paginacion(0, Paginacion.TAMANO_MAXIMO_PAGINA + 1))
                    .isInstanceOf(ValorInvalidoDominio.class)
                    .hasMessageContaining(String.valueOf(Paginacion.TAMANO_MAXIMO_PAGINA));
        }

        @Test
        void debeAceptarTamanoMaximoExacto() {
            var paginacion = new Paginacion(0, Paginacion.TAMANO_MAXIMO_PAGINA);

            assertThat(paginacion.tamano()).isEqualTo(Paginacion.TAMANO_MAXIMO_PAGINA);
        }
    }

    @Nested
    class PorDefecto {

        @Test
        void debeRetornarPaginacionConValoresPorDefecto() {
            var paginacion = Paginacion.porDefecto();

            assertThat(paginacion.pagina()).isZero();
            assertThat(paginacion.tamano()).isEqualTo(Paginacion.TAMANO_POR_DEFECTO);
        }
    }

    @Nested
    class Desplazamiento {

        @Test
        void debeRetornarCeroEnPrimeraPagina() {
            var paginacion = new Paginacion(0, 25);

            assertThat(paginacion.desplazamiento()).isZero();
        }

        @Test
        void debeMultiplicarPaginaPorTamano() {
            var paginacion = new Paginacion(3, 25);

            assertThat(paginacion.desplazamiento()).isEqualTo(75);
        }
    }
}
