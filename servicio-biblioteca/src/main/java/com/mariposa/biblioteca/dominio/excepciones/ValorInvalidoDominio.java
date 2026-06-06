package com.mariposa.biblioteca.dominio.excepciones;

public final class ValorInvalidoDominio extends ExcepcionDominio {

    private final String campo;

    public ValorInvalidoDominio(String campo, String mensaje) {
        super("Valor inválido en campo '%s': %s".formatted(campo, mensaje));
        this.campo = campo;
    }

    public String campo() {
        return campo;
    }
}
