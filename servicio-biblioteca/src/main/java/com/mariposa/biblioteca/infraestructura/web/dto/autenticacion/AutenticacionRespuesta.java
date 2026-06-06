package com.mariposa.biblioteca.infraestructura.web.dto.autenticacion;

import com.mariposa.biblioteca.infraestructura.web.dto.usuario.UsuarioRespuesta;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(
        name = "AutenticacionRespuesta",
        description = "Respuesta de autenticación exitosa con par de tokens JWT y datos del usuario."
)
public record AutenticacionRespuesta(
        @Schema(
                description = "Token JWT de acceso para autorizar peticiones a recursos protegidos.",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInRpcG8iOiJhY2Nlc28ifQ.firma",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String tokenAcceso,

        @Schema(
                description = "Token JWT de refresco para obtener nuevos tokens de acceso sin reautenticar.",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInRpcG8iOiJyZWZyZXNjbyJ9.firma",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String tokenRefresco,

        @Schema(
                description = "Instante UTC ISO-8601 en que expira el token de acceso.",
                example = "2026-06-06T12:30:00Z",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        Instant expiraEn,

        @Schema(
                description = "Datos del usuario autenticado.",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        UsuarioRespuesta usuario
) {
}
