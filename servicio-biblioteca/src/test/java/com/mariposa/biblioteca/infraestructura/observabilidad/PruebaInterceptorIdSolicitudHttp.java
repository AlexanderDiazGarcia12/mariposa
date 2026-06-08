package com.mariposa.biblioteca.infraestructura.observabilidad;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PruebaInterceptorIdSolicitudHttp {

    private InterceptorIdSolicitudHttp interceptor;
    private HttpHeaders cabeceras;
    private HttpRequest peticion;
    private ClientHttpRequestExecution ejecucion;
    private ClientHttpResponse respuestaSimulada;

    @BeforeEach
    void prepararPorPrueba() throws Exception {
        interceptor = new InterceptorIdSolicitudHttp();
        cabeceras = new HttpHeaders();
        peticion = mock(HttpRequest.class);
        given(peticion.getHeaders()).willReturn(cabeceras);
        ejecucion = mock(ClientHttpRequestExecution.class);
        respuestaSimulada = mock(ClientHttpResponse.class);
        given(ejecucion.execute(any(), any())).willReturn(respuestaSimulada);
        MDC.clear();
    }

    @AfterEach
    void limpiarMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("Añade X-Request-Id desde MDC cuando hay un id de solicitud activo")
    void agregaCabeceraCuandoHayIdEnMdc() throws Exception {
        MDC.put(FiltroIdSolicitud.CLAVE_MDC_ID_SOLICITUD, "trace-distribuido-99");

        interceptor.intercept(peticion, new byte[0], ejecucion);

        assertThat(cabeceras.getFirst(FiltroIdSolicitud.CABECERA_ID_SOLICITUD))
                .isEqualTo("trace-distribuido-99");
    }

    @Test
    @DisplayName("No añade cabecera si el MDC está vacío")
    void noAgregaCabeceraSinMdc() throws Exception {
        interceptor.intercept(peticion, new byte[0], ejecucion);

        assertThat(cabeceras.containsHeader(FiltroIdSolicitud.CABECERA_ID_SOLICITUD)).isFalse();
    }

    @Test
    @DisplayName("No sobrescribe cuando la petición ya trae X-Request-Id propio")
    void respetaCabeceraExistente() throws Exception {
        MDC.put(FiltroIdSolicitud.CLAVE_MDC_ID_SOLICITUD, "trace-mdc");
        cabeceras.add(FiltroIdSolicitud.CABECERA_ID_SOLICITUD, "trace-explicito");

        interceptor.intercept(peticion, new byte[0], ejecucion);

        assertThat(cabeceras.get(FiltroIdSolicitud.CABECERA_ID_SOLICITUD))
                .containsExactly("trace-explicito");
    }
}
