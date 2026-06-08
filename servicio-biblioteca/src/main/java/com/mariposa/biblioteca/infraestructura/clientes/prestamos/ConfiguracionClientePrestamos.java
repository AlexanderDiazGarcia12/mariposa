package com.mariposa.biblioteca.infraestructura.clientes.prestamos;

import com.mariposa.biblioteca.infraestructura.observabilidad.InterceptorIdSolicitudHttp;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(PropiedadesServicioPrestamos.class)
public class ConfiguracionClientePrestamos {

    @Bean
    public RestClient restClientPrestamos(
            PropiedadesServicioPrestamos propiedades,
            InterceptorIdSolicitudHttp interceptorIdSolicitudHttp
    ) {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(propiedades.timeoutConexionMs()));
        fabrica.setReadTimeout(Duration.ofMillis(propiedades.timeoutLecturaMs()));
        return RestClient.builder()
                .baseUrl(propiedades.urlBase())
                .requestFactory(fabrica)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor(interceptorIdSolicitudHttp)
                .build();
    }
}
