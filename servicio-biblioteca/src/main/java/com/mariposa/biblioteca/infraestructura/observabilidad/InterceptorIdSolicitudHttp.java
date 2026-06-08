package com.mariposa.biblioteca.infraestructura.observabilidad;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class InterceptorIdSolicitudHttp implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            HttpRequest peticion,
            byte[] cuerpo,
            ClientHttpRequestExecution ejecucion
    ) throws IOException {
        var idSolicitud = MDC.get(FiltroIdSolicitud.CLAVE_MDC_ID_SOLICITUD);
        if (idSolicitud != null && !idSolicitud.isBlank()
                && !peticion.getHeaders().containsHeader(FiltroIdSolicitud.CABECERA_ID_SOLICITUD)) {
            peticion.getHeaders().add(FiltroIdSolicitud.CABECERA_ID_SOLICITUD, idSolicitud);
        }
        return ejecucion.execute(peticion, cuerpo);
    }
}
