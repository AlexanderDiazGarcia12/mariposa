package com.mariposa.biblioteca.infraestructura.web.dto.comun;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(
        name = "PaginaRespuesta",
        description = "Página de resultados con metadatos de paginación."
)
public record PaginaRespuesta<T>(
        @Schema(
                description = "Elementos contenidos en la página actual.",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        List<T> elementos,

        @Schema(
                description = "Número de página actual (base 0).",
                example = "0",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        int paginaActual,

        @Schema(
                description = "Tamaño solicitado de la página.",
                example = "20",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        int tamanoPagina,

        @Schema(
                description = "Total de elementos disponibles en todo el resultado.",
                example = "138",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        long totalElementos,

        @Schema(
                description = "Total de páginas según el tamaño solicitado.",
                example = "7",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        int totalPaginas
) {
}
