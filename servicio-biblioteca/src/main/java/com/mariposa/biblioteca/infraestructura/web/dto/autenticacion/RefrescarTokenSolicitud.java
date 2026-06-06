package com.mariposa.biblioteca.infraestructura.web.dto.autenticacion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(
        name = "RefrescarTokenSolicitud",
        description = "Solicitud para renovar tokens utilizando un token de refresco vigente."
)
public record RefrescarTokenSolicitud(
        @Schema(
                description = "Token de refresco JWT emitido previamente y aún no expirado.",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c3VhcmlvIiwidGlwbyI6InJlZnJlc2NvIn0.firma",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "tokenRefresco es obligatorio") String tokenRefresco
) {
}
