package com.mariposa.biblioteca.dominio.modelo.valor;

import com.mariposa.biblioteca.dominio.excepciones.ValorInvalidoDominio;

import java.util.regex.Pattern;

public record CorreoElectronico(String valor) {

    private static final Pattern PATRON_CORREO = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    public CorreoElectronico {
        if (valor == null || valor.isBlank()) {
            throw new ValorInvalidoDominio("correoElectronico", "no puede ser nulo o vacío");
        }
        var normalizado = valor.trim().toLowerCase();
        if (!PATRON_CORREO.matcher(normalizado).matches()) {
            throw new ValorInvalidoDominio("correoElectronico", "formato de correo inválido");
        }
        valor = normalizado;
    }

    public static CorreoElectronico desTexto(String texto) {
        return new CorreoElectronico(texto);
    }
}
