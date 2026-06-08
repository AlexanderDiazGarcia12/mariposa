package com.mariposa.biblioteca.infraestructura.observabilidad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PruebaFiltroIdSolicitud {

    private FiltroIdSolicitud filtro;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain cadenaFiltros;

    @BeforeEach
    void prepararPorPrueba() {
        filtro = new FiltroIdSolicitud();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        cadenaFiltros = mock(FilterChain.class);
        MDC.clear();
    }

    @AfterEach
    void limpiarMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("Genera un UUID válido cuando no llega el encabezado X-Request-Id")
    void generaUuidCuandoNoLlegaElEncabezado() throws Exception {
        capturarIdSolicitudEnCadena();

        filtro.doFilter(request, response, cadenaFiltros);

        verify(cadenaFiltros).doFilter(request, response);
        var idGenerado = response.getHeader(FiltroIdSolicitud.CABECERA_ID_SOLICITUD);
        assertThat(idGenerado).isNotNull();
        assertThat(UUID.fromString(idGenerado)).isNotNull();
    }

    @Test
    @DisplayName("Preserva el X-Request-Id entrante si está presente")
    void preservaElEncabezadoEntrante() throws Exception {
        var idEntrante = "trace-abc-123";
        request.addHeader(FiltroIdSolicitud.CABECERA_ID_SOLICITUD, idEntrante);

        filtro.doFilter(request, response, cadenaFiltros);

        assertThat(response.getHeader(FiltroIdSolicitud.CABECERA_ID_SOLICITUD)).isEqualTo(idEntrante);
    }

    @Test
    @DisplayName("Trunca el encabezado entrante a 128 caracteres para evitar logs gigantes")
    void truncaEncabezadoEntranteDemasiadoLargo() throws Exception {
        var idGigante = "x".repeat(500);
        request.addHeader(FiltroIdSolicitud.CABECERA_ID_SOLICITUD, idGigante);

        filtro.doFilter(request, response, cadenaFiltros);

        var idDevuelto = response.getHeader(FiltroIdSolicitud.CABECERA_ID_SOLICITUD);
        assertThat(idDevuelto).hasSize(128);
    }

    @Test
    @DisplayName("Si el encabezado entrante está en blanco, genera un UUID nuevo")
    void encabezadoEnBlancoGeneraUuidNuevo() throws Exception {
        request.addHeader(FiltroIdSolicitud.CABECERA_ID_SOLICITUD, "   ");

        filtro.doFilter(request, response, cadenaFiltros);

        var idDevuelto = response.getHeader(FiltroIdSolicitud.CABECERA_ID_SOLICITUD);
        assertThat(idDevuelto).isNotBlank();
        assertThat(UUID.fromString(idDevuelto)).isNotNull();
    }

    @Test
    @DisplayName("Coloca el id en MDC durante la cadena y lo limpia después")
    void colocaIdEnMdcYLoLimpiaAlFinal() throws Exception {
        var idEntrante = "trazado-456";
        request.addHeader(FiltroIdSolicitud.CABECERA_ID_SOLICITUD, idEntrante);

        var idEnMdcDurantePeticion = new String[1];
        doAnswer(invocacion -> {
            idEnMdcDurantePeticion[0] = MDC.get(FiltroIdSolicitud.CLAVE_MDC_ID_SOLICITUD);
            return null;
        }).when(cadenaFiltros).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

        filtro.doFilter(request, response, cadenaFiltros);

        assertThat(idEnMdcDurantePeticion[0]).isEqualTo(idEntrante);
        assertThat(MDC.get(FiltroIdSolicitud.CLAVE_MDC_ID_SOLICITUD)).isNull();
    }

    @Test
    @DisplayName("Limpia el MDC incluso si la cadena lanza una excepción")
    void limpiaMdcAunCuandoLaCadenaLanzaExcepcion() throws Exception {
        doThrow(new ServletException("falla simulada"))
                .when(cadenaFiltros).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

        assertThatThrownBy(() -> filtro.doFilter(request, response, cadenaFiltros))
                .isInstanceOf(ServletException.class);

        assertThat(MDC.get(FiltroIdSolicitud.CLAVE_MDC_ID_SOLICITUD)).isNull();
    }

    private void capturarIdSolicitudEnCadena() throws IOException, ServletException {
        doAnswer(invocacion -> null).when(cadenaFiltros).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }
}
