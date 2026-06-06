package com.mariposa.biblioteca.infraestructura.web.controladores;

import com.mariposa.biblioteca.dominio.modelo.Rol;
import com.mariposa.biblioteca.dominio.puertos.salida.ProveedorToken.ClaimsToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

final class AuxiliarAutenticacionPrueba {

    private AuxiliarAutenticacionPrueba() {
    }

    static RequestPostProcessor autenticadoComo(UUID idUsuario, Rol rol) {
        var claims = new ClaimsToken(
                idUsuario,
                "usuario-de-prueba",
                rol,
                ClaimsToken.TIPO_ACCESO,
                Instant.parse("2026-06-05T10:15:00Z")
        );
        Authentication autenticacion = new UsernamePasswordAuthenticationToken(
                claims,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()))
        );
        return SecurityMockMvcRequestPostProcessors.authentication(autenticacion);
    }
}
