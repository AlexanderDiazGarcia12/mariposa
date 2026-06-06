package com.mariposa.biblioteca.infraestructura.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@Component
public class ManejadorAccesoDenegadoJwt implements AccessDeniedHandler {

    private static final URI TIPO_PROBLEMA = URI.create("urn:problema:acceso-denegado");

    private final EscritorRespuestaProblema escritor;

    public ManejadorAccesoDenegadoJwt(EscritorRespuestaProblema escritor) {
        this.escritor = escritor;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        var problema = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "No autorizado para este recurso");
        problema.setTitle("Acceso denegado");
        problema.setType(TIPO_PROBLEMA);
        problema.setInstance(URI.create(request.getRequestURI()));
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        escritor.escribir(response, problema);
    }
}
