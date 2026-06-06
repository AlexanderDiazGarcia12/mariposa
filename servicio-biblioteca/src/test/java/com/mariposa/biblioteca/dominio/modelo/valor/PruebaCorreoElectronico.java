package com.mariposa.biblioteca.dominio.modelo.valor;

import com.mariposa.biblioteca.dominio.excepciones.ValorInvalidoDominio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PruebaCorreoElectronico {

    @Nested
    class ConstructorCompacto {

        @ParameterizedTest
        @ValueSource(strings = {
                "usuario@dominio.com",
                "nombre.apellido@empresa.co",
                "usuario+tag@dominio.org",
                "u_1@dominio.io"
        })
        void debeAceptarCorreosValidos(String valor) {
            var correo = new CorreoElectronico(valor);

            assertThat(correo.valor()).isEqualTo(valor.toLowerCase());
        }

        @Test
        void debeNormalizarAMinusculasYRecortar() {
            var correo = new CorreoElectronico("  USUARIO@Dominio.COM  ");

            assertThat(correo.valor()).isEqualTo("usuario@dominio.com");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void debeRechazarValoresNulosOEnBlanco(String valor) {
            assertThatThrownBy(() -> new CorreoElectronico(valor))
                    .isInstanceOf(ValorInvalidoDominio.class)
                    .extracting("campo").isEqualTo("correoElectronico");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "sin-arroba.com",
                "usuario@",
                "@dominio.com",
                "usuario@dominio",
                "usuario@@dominio.com",
                "usuario@dominio.c"
        })
        void debeRechazarFormatosInvalidos(String valor) {
            assertThatThrownBy(() -> new CorreoElectronico(valor))
                    .isInstanceOf(ValorInvalidoDominio.class)
                    .extracting("campo").isEqualTo("correoElectronico");
        }
    }

    @Nested
    class FactoryDesTexto {

        @Test
        void debeConstruirCorreoDesdeTexto() {
            var correo = CorreoElectronico.desTexto("usuario@dominio.com");

            assertThat(correo.valor()).isEqualTo("usuario@dominio.com");
        }

        @Test
        void debeNormalizarAlConstruirDesdeTexto() {
            var correo = CorreoElectronico.desTexto("  Usuario@Dominio.COM  ");

            assertThat(correo.valor()).isEqualTo("usuario@dominio.com");
        }
    }
}
