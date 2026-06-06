package com.mariposa.biblioteca.dominio.modelo.valor;

import com.mariposa.biblioteca.dominio.excepciones.ValorInvalidoDominio;

import java.util.regex.Pattern;

public record Isbn(String valor) {

    private static final Pattern PATRON_ISBN = Pattern.compile(
            "^(?:\\d{9}[\\dXx]|\\d{13})$"
    );

    public Isbn {
        if (valor == null) {
            throw new ValorInvalidoDominio("isbn", "no puede ser nulo");
        }
        var normalizado = valor.replaceAll("[\\s-]", "");
        if (!PATRON_ISBN.matcher(normalizado).matches()) {
            throw new ValorInvalidoDominio("isbn", "formato inválido, debe ser ISBN-10 o ISBN-13");
        }
        valor = normalizado;
    }

    public static Isbn desTexto(String texto) {
        return new Isbn(texto);
    }
}
