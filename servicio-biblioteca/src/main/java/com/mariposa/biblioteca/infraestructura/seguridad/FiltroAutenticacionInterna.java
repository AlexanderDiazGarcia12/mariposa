package com.mariposa.biblioteca.infraestructura.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class FiltroAutenticacionInterna extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FiltroAutenticacionInterna.class);
    private static final String RUTA_INTERNA = "/api/v1/internal/";
    private static final String HEADER_SERVICIO_INTERNO = "X-Servicio-Interno";
    private static final String AUTORIDAD_SERVICIO_INTERNO = "ROLE_SERVICIO_INTERNO";
    private static final String PRINCIPAL_SERVICIO_INTERNO = "servicio-interno";

    private final PropiedadesServicioInterno propiedadesServicioInterno;

    public FiltroAutenticacionInterna(PropiedadesServicioInterno propiedadesServicioInterno) {
        this.propiedadesServicioInterno = propiedadesServicioInterno;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (!esRutaInterna(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        var encabezado = request.getHeader(HEADER_SERVICIO_INTERNO);
        if (esSecretoValido(encabezado)) {
            autenticarServicioInterno();
        } else {
            LOGGER.debug("Solicitud interna sin secreto válido en {}", request.getRequestURI());
        }
        filterChain.doFilter(request, response);
    }

    private boolean esRutaInterna(HttpServletRequest request) {
        return request.getRequestURI().startsWith(RUTA_INTERNA);
    }

    private boolean esSecretoValido(String encabezado) {
        return encabezado != null && encabezado.equals(propiedadesServicioInterno.secreto());
    }

    private void autenticarServicioInterno() {
        var autoridad = new SimpleGrantedAuthority(AUTORIDAD_SERVICIO_INTERNO);
        var autenticacion = new UsernamePasswordAuthenticationToken(
                PRINCIPAL_SERVICIO_INTERNO, null, List.of(autoridad)
        );
        SecurityContextHolder.getContext().setAuthentication(autenticacion);
    }
}
