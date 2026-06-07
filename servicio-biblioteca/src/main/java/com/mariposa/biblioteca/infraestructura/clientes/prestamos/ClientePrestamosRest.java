package com.mariposa.biblioteca.infraestructura.clientes.prestamos;

import com.mariposa.biblioteca.dominio.excepciones.PrestamoRechazadoPorServicioB;
import com.mariposa.biblioteca.dominio.excepciones.ServicioPrestamosNoDisponible;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.PrestamoRegistrado;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.RegistrarPrestamoComando;
import com.mariposa.biblioteca.dominio.puertos.salida.ClientePrestamos;
import com.mariposa.biblioteca.infraestructura.clientes.prestamos.DtoPrestamoServicioB.PrestamoRespuestaB;
import com.mariposa.biblioteca.infraestructura.clientes.prestamos.DtoPrestamoServicioB.RegistrarPrestamoSolicitudB;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Component
public class ClientePrestamosRest implements ClientePrestamos {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientePrestamosRest.class);
    private static final String NOMBRE_RESILENCIA = "clientePrestamos";
    private static final String RUTA_PRESTAMOS = "/api/v1/prestamos";

    private final RestClient restClientPrestamos;

    public ClientePrestamosRest(
            @org.springframework.beans.factory.annotation.Qualifier("restClientPrestamos")
            RestClient restClientPrestamos
    ) {
        this.restClientPrestamos = restClientPrestamos;
    }

    @Override
    @CircuitBreaker(name = NOMBRE_RESILENCIA, fallbackMethod = "registrarFallback")
    @Retry(name = NOMBRE_RESILENCIA)
    public PrestamoRegistrado registrar(RegistrarPrestamoComando comando) {
        var solicitud = aSolicitud(comando);
        var respuesta = restClientPrestamos.post()
                .uri(RUTA_PRESTAMOS)
                .body(solicitud)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::manejarErrorCliente)
                .body(PrestamoRespuestaB.class);
        if (respuesta == null) {
            throw new ServicioPrestamosNoDisponible("Respuesta vacía del servicio de préstamos");
        }
        return aDominio(respuesta);
    }

    private void manejarErrorCliente(
            org.springframework.http.HttpRequest peticion,
            org.springframework.http.client.ClientHttpResponse respuesta
    ) throws java.io.IOException {
        var estado = respuesta.getStatusCode();
        var cuerpo = leerCuerpo(respuesta);
        var detalle = extraerDetalle(cuerpo, estado);
        var codigo = String.valueOf(estado.value());
        LOGGER.warn("Servicio de préstamos rechazó la solicitud [{}]: {}", codigo, detalle);
        throw new PrestamoRechazadoPorServicioB(codigo, detalle);
    }

    private String leerCuerpo(org.springframework.http.client.ClientHttpResponse respuesta) {
        try {
            return new String(respuesta.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            return "";
        }
    }

    private String extraerDetalle(String cuerpo, HttpStatusCode estado) {
        if (cuerpo == null || cuerpo.isBlank()) {
            return "Servicio de préstamos respondió código %d sin cuerpo".formatted(estado.value());
        }
        try {
            var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
            var nodo = mapper.readTree(cuerpo);
            if (nodo.has("detail")) {
                return nodo.get("detail").asString();
            }
            if (nodo.has("title")) {
                return nodo.get("title").asString();
            }
            return cuerpo;
        } catch (Exception e) {
            return cuerpo;
        }
    }

    @SuppressWarnings("unused")
    private PrestamoRegistrado registrarFallback(RegistrarPrestamoComando comando, Throwable causa) {
        switch (causa) {
            case PrestamoRechazadoPorServicioB rechazo -> throw rechazo;
            case HttpClientErrorException error -> throw new PrestamoRechazadoPorServicioB(
                    String.valueOf(error.getStatusCode().value()),
                    error.getMessage()
            );
            case CallNotPermittedException cnp -> throw new ServicioPrestamosNoDisponible(
                    "Circuit breaker abierto para el servicio de préstamos", cnp
            );
            case ResourceAccessException rae -> throw new ServicioPrestamosNoDisponible(
                    "No se pudo contactar al servicio de préstamos: " + rae.getMessage(), rae
            );
            case HttpServerErrorException he -> throw new ServicioPrestamosNoDisponible(
                    "Servicio de préstamos respondió error de servidor: " + he.getStatusCode(), he
            );
            default -> throw new ServicioPrestamosNoDisponible(
                    "Fallo invocando al servicio de préstamos: " + causa.getMessage(), causa
            );
        }
    }

    private static RegistrarPrestamoSolicitudB aSolicitud(RegistrarPrestamoComando comando) {
        return new RegistrarPrestamoSolicitudB(
                comando.idUsuario().toString(),
                comando.idLibro().toString(),
                comando.fechaPrestamo().format(DateTimeFormatter.ISO_LOCAL_DATE),
                comando.fechaDevolucionEstimada().format(DateTimeFormatter.ISO_LOCAL_DATE)
        );
    }

    private static PrestamoRegistrado aDominio(PrestamoRespuestaB respuesta) {
        return new PrestamoRegistrado(
                UUID.fromString(respuesta.id()),
                UUID.fromString(respuesta.idUsuario()),
                UUID.fromString(respuesta.idLibro()),
                LocalDate.parse(respuesta.fechaPrestamo()),
                LocalDate.parse(respuesta.fechaDevolucionEstimada()),
                respuesta.estado(),
                parsearInstante(respuesta.fechaCreacion())
        );
    }

    private static Instant parsearInstante(String texto) {
        if (texto == null || texto.isBlank()) {
            return Instant.now();
        }
        try {
            return OffsetDateTime.parse(texto).toInstant();
        } catch (DateTimeParseException primero) {
            try {
                return Instant.parse(texto);
            } catch (DateTimeParseException segundo) {
                return Instant.now();
            }
        }
    }

}
