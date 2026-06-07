package com.mariposa.biblioteca.infraestructura.seguridad;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PruebaFiltroLimiteTasaInicioSesion {

    private static final String RUTA_INICIO_SESION = "/api/v1/autenticacion/iniciar-sesion";
    private static final String IP_CLIENTE = "203.0.113.10";

    private FilterChain cadenaFiltros;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void prepararPorPrueba() {
        cadenaFiltros = mock(FilterChain.class);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setMethod("POST");
        request.setRequestURI(RUTA_INICIO_SESION);
        request.setRemoteAddr(IP_CLIENTE);
    }

    @Test
    @DisplayName("Cuando el filtro está deshabilitado, deja pasar todas las solicitudes sin consumir tokens")
    void cuandoElFiltroEstaDeshabilitadoDejaPasar() throws Exception {
        var filtro = construirFiltro(false, 1);

        filtro.doFilter(request, response, cadenaFiltros);
        filtro.doFilter(request, response, cadenaFiltros);
        filtro.doFilter(request, response, cadenaFiltros);

        verify(cadenaFiltros, times(3)).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("Ignora rutas distintas a /iniciar-sesion")
    void ignoraRutasDistintasDeInicioSesion() throws Exception {
        var filtro = construirFiltro(true, 1);
        request.setRequestURI("/api/v1/libros");

        filtro.doFilter(request, response, cadenaFiltros);

        verify(cadenaFiltros).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("Ignora métodos distintos a POST sobre la ruta de inicio de sesión")
    void ignoraMetodosDistintosDePost() throws Exception {
        var filtro = construirFiltro(true, 1);
        request.setMethod("GET");

        filtro.doFilter(request, response, cadenaFiltros);

        verify(cadenaFiltros).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("Permite N solicitudes hasta agotar la capacidad y luego responde 429 con Retry-After")
    void permiteHastaCapacidadYLuegoResponde429() throws Exception {
        var filtro = construirFiltro(true, 2);

        filtro.doFilter(request, response, cadenaFiltros);
        filtro.doFilter(request, new MockHttpServletResponse(), cadenaFiltros);

        var responseExcedida = new MockHttpServletResponse();
        filtro.doFilter(request, responseExcedida, cadenaFiltros);

        verify(cadenaFiltros, times(2)).doFilter(any(), any());
        verify(cadenaFiltros, never()).doFilter(request, responseExcedida);
        assertThat(responseExcedida.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(responseExcedida.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(responseExcedida.getHeader(HttpHeaders.RETRY_AFTER)).isNotNull();
        assertThat(Long.parseLong(responseExcedida.getHeader(HttpHeaders.RETRY_AFTER))).isGreaterThanOrEqualTo(1L);
        assertThat(responseExcedida.getHeader("X-Rate-Limit-Remaining")).isEqualTo("0");
        assertThat(responseExcedida.getContentAsString()).contains("Demasiados intentos");
    }

    @Test
    @DisplayName("Buckets independientes por IP: una IP agotada no afecta a otra")
    void bucketsIndependientesPorIp() throws Exception {
        var filtro = construirFiltro(true, 1);

        filtro.doFilter(request, response, cadenaFiltros);

        var requestSegundaIp = new MockHttpServletRequest();
        requestSegundaIp.setMethod("POST");
        requestSegundaIp.setRequestURI(RUTA_INICIO_SESION);
        requestSegundaIp.setRemoteAddr("198.51.100.20");
        var responseSegundaIp = new MockHttpServletResponse();
        filtro.doFilter(requestSegundaIp, responseSegundaIp, cadenaFiltros);

        verify(cadenaFiltros).doFilter(request, response);
        verify(cadenaFiltros).doFilter(requestSegundaIp, responseSegundaIp);
        assertThat(responseSegundaIp.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("Usa el primer valor de X-Forwarded-For como clave si está presente")
    void usaXForwardedForCuandoEstaPresente() throws Exception {
        var filtro = construirFiltro(true, 1);
        request.addHeader("X-Forwarded-For", "10.0.0.7, 192.168.1.1");

        filtro.doFilter(request, response, cadenaFiltros);

        var requestOtroProxy = new MockHttpServletRequest();
        requestOtroProxy.setMethod("POST");
        requestOtroProxy.setRequestURI(RUTA_INICIO_SESION);
        requestOtroProxy.setRemoteAddr(IP_CLIENTE);
        requestOtroProxy.addHeader("X-Forwarded-For", "10.0.0.7");
        var responseOtroProxy = new MockHttpServletResponse();
        filtro.doFilter(requestOtroProxy, responseOtroProxy, cadenaFiltros);

        verify(cadenaFiltros).doFilter(request, response);
        verify(cadenaFiltros, never()).doFilter(requestOtroProxy, responseOtroProxy);
        assertThat(responseOtroProxy.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    @DisplayName("Incluye encabezado X-Rate-Limit-Remaining con tokens restantes en respuestas permitidas")
    void incluyeEncabezadoConTokensRestantes() throws Exception {
        var filtro = construirFiltro(true, 3);

        filtro.doFilter(request, response, cadenaFiltros);

        verify(cadenaFiltros).doFilter(request, response);
        assertThat(response.getHeader("X-Rate-Limit-Remaining")).isEqualTo("2");
    }

    private FiltroLimiteTasaInicioSesion construirFiltro(boolean habilitado, int capacidad) {
        var propiedades = new PropiedadesLimiteTasaInicioSesion(habilitado, capacidad, 60);
        var registro = new RegistroLimitadoresTasa(propiedades);
        var escritor = new EscritorRespuestaProblema();
        return new FiltroLimiteTasaInicioSesion(propiedades, registro, escritor);
    }
}
