package com.mariposa.biblioteca.dominio.modelo.consultas;

import java.util.List;
import java.util.Objects;

public record PaginaResultado<T>(
        List<T> elementos,
        int paginaActual,
        int tamanoPagina,
        long totalElementos,
        int totalPaginas
) {

    public PaginaResultado {
        elementos = List.copyOf(Objects.requireNonNull(elementos, "elementos no puede ser nulo"));
    }

    public static <T> PaginaResultado<T> de(List<T> elementos, Paginacion paginacion, long totalElementos) {
        var totalPaginas = calcularTotalPaginas(totalElementos, paginacion.tamano());
        return new PaginaResultado<>(
                elementos,
                paginacion.pagina(),
                paginacion.tamano(),
                totalElementos,
                totalPaginas
        );
    }

    public static <T> PaginaResultado<T> vacia(Paginacion paginacion) {
        return new PaginaResultado<>(List.of(), paginacion.pagina(), paginacion.tamano(), 0L, 0);
    }

    private static int calcularTotalPaginas(long totalElementos, int tamano) {
        if (totalElementos == 0) {
            return 0;
        }
        return (int) Math.ceilDiv(totalElementos, (long) tamano);
    }
}
