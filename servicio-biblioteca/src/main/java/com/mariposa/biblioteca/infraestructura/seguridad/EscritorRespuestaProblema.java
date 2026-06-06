package com.mariposa.biblioteca.infraestructura.seguridad;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

@Component
public class EscritorRespuestaProblema {

    private final ObjectMapper objectMapper;

    public EscritorRespuestaProblema() {
        this.objectMapper = JsonMapper.builder().build();
    }

    public void escribir(HttpServletResponse response, ProblemDetail problema) throws IOException {
        objectMapper.writeValue(response.getOutputStream(), problema);
    }
}
