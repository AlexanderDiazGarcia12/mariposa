package com.mariposa.biblioteca.infraestructura.web.dto.autenticacion;

import jakarta.validation.constraints.NotBlank;

public record RefrescarTokenSolicitud(
        @NotBlank(message = "tokenRefresco es obligatorio") String tokenRefresco
) {
}
