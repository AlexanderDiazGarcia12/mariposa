package com.mariposa.biblioteca.infraestructura.seguridad;

import com.mariposa.biblioteca.dominio.excepciones.CredencialesInvalidas;
import com.mariposa.biblioteca.dominio.puertos.salida.ProveedorToken;
import com.mariposa.biblioteca.dominio.puertos.salida.ProveedorToken.ClaimsToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class FiltroAutenticacionJwt extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FiltroAutenticacionJwt.class);
    private static final String PREFIJO_BEARER = "Bearer ";

    private final ProveedorToken proveedorToken;

    public FiltroAutenticacionJwt(ProveedorToken proveedorToken) {
        this.proveedorToken = proveedorToken;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        extraerToken(request).flatMap(this::validarSilencioso)
                .filter(ClaimsToken::esAcceso)
                .ifPresent(this::autenticar);
        filterChain.doFilter(request, response);
    }

    private Optional<String> extraerToken(HttpServletRequest request) {
        var encabezado = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (encabezado == null || !encabezado.startsWith(PREFIJO_BEARER)) {
            return Optional.empty();
        }
        var token = encabezado.substring(PREFIJO_BEARER.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }

    private Optional<ClaimsToken> validarSilencioso(String token) {
        try {
            return Optional.of(proveedorToken.validar(token));
        } catch (CredencialesInvalidas e) {
            LOGGER.debug("Token JWT inválido en filtro: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private void autenticar(ClaimsToken claims) {
        var autoridad = new SimpleGrantedAuthority("ROLE_" + claims.rol().name());
        var autenticacion = new UsernamePasswordAuthenticationToken(claims, null, List.of(autoridad));
        SecurityContextHolder.getContext().setAuthentication(autenticacion);
    }
}
