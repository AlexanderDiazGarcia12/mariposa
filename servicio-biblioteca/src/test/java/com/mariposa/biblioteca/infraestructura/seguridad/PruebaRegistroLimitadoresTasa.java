package com.mariposa.biblioteca.infraestructura.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PruebaRegistroLimitadoresTasa {

    private static final PropiedadesLimiteTasaInicioSesion PROPIEDADES =
            new PropiedadesLimiteTasaInicioSesion(true, 5, 60);

    @Test
    @DisplayName("Devuelve el mismo bucket para la misma clave en llamadas sucesivas")
    void devuelveElMismoBucketParaLaMismaClave() {
        var registro = new RegistroLimitadoresTasa(PROPIEDADES);

        var bucketPrimero = registro.resolverInicioSesion("192.168.1.10");
        var bucketSegundo = registro.resolverInicioSesion("192.168.1.10");

        assertThat(bucketPrimero).isSameAs(bucketSegundo);
    }

    @Test
    @DisplayName("Genera buckets independientes para claves distintas")
    void generaBucketsIndependientesParaClavesDistintas() {
        var registro = new RegistroLimitadoresTasa(PROPIEDADES);

        var bucketCliente1 = registro.resolverInicioSesion("192.168.1.10");
        var bucketCliente2 = registro.resolverInicioSesion("192.168.1.20");

        assertThat(bucketCliente1).isNotSameAs(bucketCliente2);
    }

    @Test
    @DisplayName("El bucket nuevo tiene la capacidad configurada")
    void elBucketNuevoTieneLaCapacidadConfigurada() {
        var propiedades = new PropiedadesLimiteTasaInicioSesion(true, 3, 60);
        var registro = new RegistroLimitadoresTasa(propiedades);

        var bucket = registro.resolverInicioSesion("cliente");

        assertThat(bucket.getAvailableTokens()).isEqualTo(3);
    }

    @Test
    @DisplayName("Se rechazan llamadas tras agotar todos los tokens disponibles")
    void seRechazanLlamadasTrasAgotarLosTokens() {
        var propiedades = new PropiedadesLimiteTasaInicioSesion(true, 2, 60);
        var registro = new RegistroLimitadoresTasa(propiedades);

        var bucket = registro.resolverInicioSesion("cliente");

        assertThat(bucket.tryConsume(1)).isTrue();
        assertThat(bucket.tryConsume(1)).isTrue();
        assertThat(bucket.tryConsume(1)).isFalse();
    }
}
