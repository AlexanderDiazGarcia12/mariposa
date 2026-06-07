package com.mariposa.biblioteca.infraestructura.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

@Component
public class FiltroLimiteTasaInicioSesion extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FiltroLimiteTasaInicioSesion.class);
    private static final String RUTA_INICIO_SESION = "/api/v1/autenticacion/iniciar-sesion";
    private static final String METODO_POST = "POST";
    private static final String CABECERA_REINTENTAR_DESPUES = "Retry-After";
    private static final String CABECERA_TOKENS_RESTANTES = "X-Rate-Limit-Remaining";
    private static final String CABECERA_REENVIO_REAL = "X-Forwarded-For";
    private static final String CLAVE_DESCONOCIDA = "desconocida";
    private static final URI TIPO_PROBLEMA_LIMITE_EXCEDIDO = URI.create("urn:problema:limite-tasa-excedido");

    private final PropiedadesLimiteTasaInicioSesion propiedades;
    private final RegistroLimitadoresTasa registroLimitadoresTasa;
    private final EscritorRespuestaProblema escritorRespuestaProblema;

    public FiltroLimiteTasaInicioSesion(
            PropiedadesLimiteTasaInicioSesion propiedades,
            RegistroLimitadoresTasa registroLimitadoresTasa,
            EscritorRespuestaProblema escritorRespuestaProblema
    ) {
        this.propiedades = propiedades;
        this.registroLimitadoresTasa = registroLimitadoresTasa;
        this.escritorRespuestaProblema = escritorRespuestaProblema;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !propiedades.habilitado()
                || !METODO_POST.equalsIgnoreCase(request.getMethod())
                || !RUTA_INICIO_SESION.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        var clave = resolverClaveCliente(request);
        var bucket = registroLimitadoresTasa.resolverInicioSesion(clave);
        var sonda = bucket.tryConsumeAndReturnRemaining(1);

        if (sonda.isConsumed()) {
            response.setHeader(CABECERA_TOKENS_RESTANTES, Long.toString(sonda.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        var segundosRestantes = Math.max(1L, TimeUnit.NANOSECONDS.toSeconds(sonda.getNanosToWaitForRefill()));
        LOGGER.warn("Límite de intentos de inicio de sesión excedido para clave={} reintentarEn={}s", clave, segundosRestantes);
        escribirRespuestaLimiteExcedido(response, request.getRequestURI(), segundosRestantes);
    }

    private String resolverClaveCliente(HttpServletRequest request) {
        var encabezadoReenviado = request.getHeader(CABECERA_REENVIO_REAL);
        if (encabezadoReenviado != null && !encabezadoReenviado.isBlank()) {
            return encabezadoReenviado.split(",")[0].trim();
        }
        var direccionRemota = request.getRemoteAddr();
        return direccionRemota == null || direccionRemota.isBlank() ? CLAVE_DESCONOCIDA : direccionRemota;
    }

    private void escribirRespuestaLimiteExcedido(
            HttpServletResponse response,
            String ruta,
            long segundosRestantes
    ) throws IOException {
        var problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "Demasiados intentos de inicio de sesión. Inténtalo más tarde."
        );
        problema.setTitle("Límite de tasa excedido");
        problema.setType(TIPO_PROBLEMA_LIMITE_EXCEDIDO);
        problema.setInstance(URI.create(ruta));
        problema.setProperty("reintentarEnSegundos", segundosRestantes);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(segundosRestantes));
        response.setHeader(CABECERA_TOKENS_RESTANTES, "0");
        escritorRespuestaProblema.escribir(response, problema);
    }
}
