package com.mariposa.biblioteca.infraestructura.seguridad;

import com.mariposa.biblioteca.dominio.excepciones.CredencialesInvalidas;
import com.mariposa.biblioteca.dominio.modelo.Rol;
import com.mariposa.biblioteca.dominio.puertos.salida.ProveedorToken;
import com.mariposa.biblioteca.dominio.puertos.salida.ProveedorToken.ClaimsToken;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PruebaFiltroAutenticacionJwt {

    private static final UUID ID_USUARIO = UUID.fromString("ffffffff-1111-2222-3333-444444444444");

    @Mock
    private ProveedorToken proveedorToken;
    @Mock
    private FilterChain cadenaFiltros;

    private FiltroAutenticacionJwt filtro;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void inicializar() {
        filtro = new FiltroAutenticacionJwt(proveedorToken);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("sin header Authorization no autentica pero continúa la cadena")
    void sinHeaderNoAutenticaPeroContinua() throws Exception {
        filtro.doFilterInternal(request, response, cadenaFiltros);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(cadenaFiltros).doFilter(request, response);
    }

    @Test
    @DisplayName("con header Bearer y token de acceso válido, autentica con rol correcto")
    void conTokenAccesoValidoAutentica() throws Exception {
        var claims = new ClaimsToken(
                ID_USUARIO, "ana.gomez", Rol.ADMIN,
                ClaimsToken.TIPO_ACCESO, Instant.parse("2026-06-05T10:15:00Z")
        );
        given(proveedorToken.validar("token-valido")).willReturn(claims);
        request.addHeader("Authorization", "Bearer token-valido");

        filtro.doFilterInternal(request, response, cadenaFiltros);

        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        assertThat(autenticacion).isNotNull();
        assertThat(autenticacion.getPrincipal()).isEqualTo(claims);
        assertThat(autenticacion.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
        verify(cadenaFiltros).doFilter(request, response);
    }

    @Test
    @DisplayName("con header Bearer y token inválido no autentica pero continúa la cadena")
    void conTokenInvalidoNoAutenticaPeroContinua() throws Exception {
        given(proveedorToken.validar("token-malo")).willThrow(new CredencialesInvalidas());
        request.addHeader("Authorization", "Bearer token-malo");

        filtro.doFilterInternal(request, response, cadenaFiltros);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(cadenaFiltros).doFilter(request, response);
    }

    @Test
    @DisplayName("con header Bearer y token tipo refresco no autentica")
    void conTokenTipoRefrescoNoAutentica() throws Exception {
        var claims = new ClaimsToken(
                ID_USUARIO, null, null,
                ClaimsToken.TIPO_REFRESCO, Instant.parse("2026-06-12T10:00:00Z")
        );
        given(proveedorToken.validar("token-refresco")).willReturn(claims);
        request.addHeader("Authorization", "Bearer token-refresco");

        filtro.doFilterInternal(request, response, cadenaFiltros);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(cadenaFiltros).doFilter(request, response);
    }

    @Test
    @DisplayName("con header Authorization sin prefijo Bearer no consulta al proveedor")
    void sinPrefijoBearerIgnoraHeader() throws Exception {
        request.addHeader("Authorization", "Basic abc123");

        filtro.doFilterInternal(request, response, cadenaFiltros);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(cadenaFiltros).doFilter(request, response);
        verify(proveedorToken, org.mockito.Mockito.never()).validar(any());
    }
}
