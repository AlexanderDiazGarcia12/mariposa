package com.mariposa.biblioteca.dominio.puertos.entrada.comandos;

import com.mariposa.biblioteca.dominio.modelo.Usuario;

import java.time.Instant;

public record ResultadoAutenticacion(
        String tokenAcceso,
        String tokenRefresco,
        Instant expiraEn,
        Usuario usuario
) {
}
