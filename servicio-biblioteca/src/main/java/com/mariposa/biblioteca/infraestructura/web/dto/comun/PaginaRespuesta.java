package com.mariposa.biblioteca.infraestructura.web.dto.comun;

import java.util.List;

public record PaginaRespuesta<T>(
        List<T> elementos,
        int paginaActual,
        int tamanoPagina,
        long totalElementos,
        int totalPaginas
) {
}
