package com.mariposa.biblioteca.dominio.modelo.consultas;

import com.mariposa.biblioteca.dominio.excepciones.ValorInvalidoDominio;

public record Paginacion(int pagina, int tamano) {

    public static final int TAMANO_POR_DEFECTO = 20;
    public static final int TAMANO_MAXIMO_PAGINA = 100;

    public Paginacion {
        if (pagina < 0) {
            throw new ValorInvalidoDominio("pagina", "no puede ser negativa");
        }
        if (tamano <= 0) {
            throw new ValorInvalidoDominio("tamano", "debe ser mayor que cero");
        }
        if (tamano > TAMANO_MAXIMO_PAGINA) {
            throw new ValorInvalidoDominio(
                    "tamano",
                    "no puede exceder %d".formatted(TAMANO_MAXIMO_PAGINA)
            );
        }
    }

    public static Paginacion porDefecto() {
        return new Paginacion(0, TAMANO_POR_DEFECTO);
    }

    public int desplazamiento() {
        return pagina * tamano;
    }
}
