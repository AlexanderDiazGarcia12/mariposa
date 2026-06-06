package com.mariposa.biblioteca.infraestructura.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@Component
public class PuntoEntradaAutenticacionJwt implements AuthenticationEntryPoint {

    private static final URI TIPO_PROBLEMA = URI.create("urn:problema:autenticacion-requerida");

    private final EscritorRespuestaProblema escritor;

    public PuntoEntradaAutenticacionJwt(EscritorRespuestaProblema escritor) {
        this.escritor = escritor;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        var problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Autenticación requerida");
        problema.setTitle("No autenticado");
        problema.setType(TIPO_PROBLEMA);
        problema.setInstance(URI.create(request.getRequestURI()));
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        escritor.escribir(response, problema);
    }
}
