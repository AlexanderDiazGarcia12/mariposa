package com.mariposa.biblioteca.infraestructura.clientes.prestamos;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.mariposa.biblioteca.dominio.excepciones.PrestamoRechazadoPorServicioB;
import com.mariposa.biblioteca.dominio.excepciones.ServicioPrestamosNoDisponible;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.RegistrarPrestamoComando;
import com.mariposa.biblioteca.infraestructura.observabilidad.InterceptorIdSolicitudHttp;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = PruebaClientePrestamosRest.ContextoMinimo.class,
        properties = {
                "spring.main.web-application-type=none",
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
                        "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration," +
                        "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration," +
                        "org.springframework.boot.orm.jpa.autoconfigure.HibernateJpaAutoConfiguration," +
                        "org.springframework.boot.data.jpa.autoconfigure.JpaRepositoriesAutoConfiguration",
                "servicio.prestamos.timeout-conexion-ms=500",
                "servicio.prestamos.timeout-lectura-ms=800",
                "resilience4j.circuitbreaker.instances.clientePrestamos.slidingWindowSize=10",
                "resilience4j.circuitbreaker.instances.clientePrestamos.failureRateThreshold=80",
                "resilience4j.circuitbreaker.instances.clientePrestamos.minimumNumberOfCalls=20",
                "resilience4j.circuitbreaker.instances.clientePrestamos.waitDurationInOpenState=10s",
                "resilience4j.retry.instances.clientePrestamos.maxAttempts=2",
                "resilience4j.retry.instances.clientePrestamos.waitDuration=50ms"
        }
)
class PruebaClientePrestamosRest {

    private static final UUID ID_USUARIO = UUID.fromString("aaaaaaaa-1111-2222-3333-cccccccccccc");
    private static final UUID ID_LIBRO = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID ID_PRESTAMO = UUID.fromString("99999999-aaaa-bbbb-cccc-dddddddddddd");
    private static final LocalDate FECHA_PRESTAMO = LocalDate.parse("2026-06-06");
    private static final LocalDate FECHA_DEVOLUCION = LocalDate.parse("2026-06-20");

    private static final WireMockServer WIRE_MOCK = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        WIRE_MOCK.start();
        com.github.tomakehurst.wiremock.client.WireMock.configureFor("localhost", WIRE_MOCK.port());
    }

    @Autowired
    private ClientePrestamosRest cliente;

    @AfterEach
    void reiniciarMocks() {
        WIRE_MOCK.resetAll();
    }

    @AfterAll
    static void detenerWireMock() {
        WIRE_MOCK.stop();
    }

    @DynamicPropertySource
    static void propiedadesDinamicas(DynamicPropertyRegistry registry) {
        registry.add("servicio.prestamos.url-base", () -> "http://localhost:" + WIRE_MOCK.port());
    }

    @Test
    @DisplayName("registrar devuelve PrestamoRegistrado cuando el Servicio B responde 201")
    void registrarCaminoFeliz() {
        stubFor(post(urlEqualTo("/api/v1/prestamos"))
                .withRequestBody(equalToJson("""
                        {
                          "idUsuario": "%s",
                          "idLibro": "%s",
                          "fechaPrestamo": "2026-06-06",
                          "fechaDevolucionEstimada": "2026-06-20"
                        }
                        """.formatted(ID_USUARIO, ID_LIBRO)))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": "%s",
                                  "idUsuario": "%s",
                                  "idLibro": "%s",
                                  "fechaPrestamo": "2026-06-06",
                                  "fechaDevolucionEstimada": "2026-06-20",
                                  "fechaDevolucionReal": null,
                                  "estado": "ACTIVO",
                                  "estaAtrasado": false,
                                  "fechaCreacion": "2026-06-06T10:00:00Z",
                                  "fechaActualizacion": "2026-06-06T10:00:00Z"
                                }
                                """.formatted(ID_PRESTAMO, ID_USUARIO, ID_LIBRO))));

        var comando = new RegistrarPrestamoComando(ID_USUARIO, ID_LIBRO, FECHA_PRESTAMO, FECHA_DEVOLUCION);
        var resultado = cliente.registrar(comando);

        assertThat(resultado.idPrestamo()).isEqualTo(ID_PRESTAMO);
        assertThat(resultado.idUsuario()).isEqualTo(ID_USUARIO);
        assertThat(resultado.idLibro()).isEqualTo(ID_LIBRO);
        assertThat(resultado.estado()).isEqualTo("ACTIVO");
        assertThat(resultado.fechaPrestamo()).isEqualTo(FECHA_PRESTAMO);
    }

    @Test
    @DisplayName("registrar lanza PrestamoRechazadoPorServicioB con código 404 cuando el Servicio B responde 404")
    void registrarRechazado404() {
        stubFor(post(urlEqualTo("/api/v1/prestamos"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/problem+json")
                        .withBody("""
                                {"type":"urn:problema:no-encontrado","title":"No encontrado","status":404,"detail":"libro inexistente"}
                                """)));

        var comando = new RegistrarPrestamoComando(ID_USUARIO, ID_LIBRO, FECHA_PRESTAMO, FECHA_DEVOLUCION);

        assertThatThrownBy(() -> cliente.registrar(comando))
                .isInstanceOf(PrestamoRechazadoPorServicioB.class)
                .hasMessageContaining("404")
                .hasMessageContaining("libro inexistente");
    }

    @Test
    @DisplayName("registrar lanza PrestamoRechazadoPorServicioB con código 422 cuando el Servicio B responde 422")
    void registrarRechazado422() {
        stubFor(post(urlEqualTo("/api/v1/prestamos"))
                .willReturn(aResponse()
                        .withStatus(422)
                        .withHeader("Content-Type", "application/problem+json")
                        .withBody("""
                                {"type":"urn:problema:validacion","title":"Validación","status":422,"detail":"usuario con préstamos vencidos"}
                                """)));

        var comando = new RegistrarPrestamoComando(ID_USUARIO, ID_LIBRO, FECHA_PRESTAMO, FECHA_DEVOLUCION);

        assertThatThrownBy(() -> cliente.registrar(comando))
                .isInstanceOf(PrestamoRechazadoPorServicioB.class)
                .hasMessageContaining("422")
                .hasMessageContaining("usuario con préstamos vencidos");
    }

    @Test
    @DisplayName("registrar lanza ServicioPrestamosNoDisponible tras reintentos cuando el Servicio B responde 503")
    void registrarServicioNoDisponiblePor503() {
        stubFor(post(urlEqualTo("/api/v1/prestamos"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withBody("servicio no disponible")));

        var comando = new RegistrarPrestamoComando(ID_USUARIO, ID_LIBRO, FECHA_PRESTAMO, FECHA_DEVOLUCION);

        assertThatThrownBy(() -> cliente.registrar(comando))
                .isInstanceOf(ServicioPrestamosNoDisponible.class);
    }

    @Test
    @DisplayName("registrar lanza ServicioPrestamosNoDisponible cuando ocurre timeout de lectura")
    void registrarTimeout() {
        stubFor(post(urlEqualTo("/api/v1/prestamos"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withFixedDelay(2000)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        var comando = new RegistrarPrestamoComando(ID_USUARIO, ID_LIBRO, FECHA_PRESTAMO, FECHA_DEVOLUCION);

        assertThatThrownBy(() -> cliente.registrar(comando))
                .isInstanceOf(ServicioPrestamosNoDisponible.class);
    }

    @org.springframework.boot.autoconfigure.SpringBootApplication
    @ComponentScan(
            basePackages = {
                    "com.mariposa.biblioteca.infraestructura.clientes.prestamos",
                    "com.mariposa.biblioteca.infraestructura.observabilidad"
            },
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = {
                            ClientePrestamosRest.class,
                            ConfiguracionClientePrestamos.class,
                            InterceptorIdSolicitudHttp.class
                    }
            )
    )
    static class ContextoMinimo {
    }
}
