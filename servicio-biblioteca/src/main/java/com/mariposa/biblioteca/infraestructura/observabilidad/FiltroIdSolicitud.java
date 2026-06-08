package com.mariposa.biblioteca.infraestructura.observabilidad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroIdSolicitud extends OncePerRequestFilter {

    static final String CABECERA_ID_SOLICITUD = "X-Request-Id";
    static final String CLAVE_MDC_ID_SOLICITUD = "idSolicitud";
    private static final int LONGITUD_MAXIMA_ID = 128;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        var idSolicitud = obtenerOGenerarId(request);
        MDC.put(CLAVE_MDC_ID_SOLICITUD, idSolicitud);
        response.setHeader(CABECERA_ID_SOLICITUD, idSolicitud);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CLAVE_MDC_ID_SOLICITUD);
        }
    }

    private String obtenerOGenerarId(HttpServletRequest request) {
        var entrante = request.getHeader(CABECERA_ID_SOLICITUD);
        if (entrante == null) {
            return UUID.randomUUID().toString();
        }
        var saneado = entrante.trim();
        if (saneado.isEmpty()) {
            return UUID.randomUUID().toString();
        }
        return saneado.length() > LONGITUD_MAXIMA_ID
                ? saneado.substring(0, LONGITUD_MAXIMA_ID)
                : saneado;
    }
}
