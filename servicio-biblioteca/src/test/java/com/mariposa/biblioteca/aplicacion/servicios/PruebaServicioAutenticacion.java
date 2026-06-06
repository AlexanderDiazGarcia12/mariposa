package com.mariposa.biblioteca.aplicacion.servicios;

import com.mariposa.biblioteca.dominio.excepciones.CredencialesInvalidas;
import com.mariposa.biblioteca.dominio.modelo.EstadoUsuario;
import com.mariposa.biblioteca.dominio.modelo.Rol;
import com.mariposa.biblioteca.dominio.modelo.Usuario;
import com.mariposa.biblioteca.dominio.modelo.valor.CorreoElectronico;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.CredencialesComando;
import com.mariposa.biblioteca.dominio.puertos.salida.CodificadorContrasena;
import com.mariposa.biblioteca.dominio.puertos.salida.ProveedorToken;
import com.mariposa.biblioteca.dominio.puertos.salida.ProveedorToken.ClaimsToken;
import com.mariposa.biblioteca.dominio.puertos.salida.ProveedorToken.TokenGenerado;
import com.mariposa.biblioteca.dominio.puertos.salida.RepositorioUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PruebaServicioAutenticacion {

    private static final UUID ID_USUARIO = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final String NOMBRE_USUARIO = "diana.lopez";
    private static final String CONTRASENA_PLANA = "Contrasena.Segura.123";
    private static final String CONTRASENA_HASH = "$2a$10$hashbcryptficticio0123456789ABCDEFGHIJKLMNOPQRSTUV";
    private static final Instant EXPIRA_ACCESO = Instant.parse("2026-06-05T10:15:00Z");
    private static final Instant EXPIRA_REFRESCO = Instant.parse("2026-06-12T10:00:00Z");

    @Mock
    private RepositorioUsuario repositorioUsuario;
    @Mock
    private CodificadorContrasena codificadorContrasena;
    @Mock
    private ProveedorToken proveedorToken;

    @InjectMocks
    private ServicioAutenticacion servicio;

    private Usuario usuarioActivo;

    @BeforeEach
    void inicializar() {
        usuarioActivo = construirUsuario(EstadoUsuario.ACTIVO);
    }

    @Test
    @DisplayName("iniciarSesion devuelve tokens cuando las credenciales son válidas y el usuario está activo")
    void iniciarSesionExitoso() {
        given(repositorioUsuario.obtenerPorNombreUsuario(NOMBRE_USUARIO)).willReturn(Optional.of(usuarioActivo));
        given(codificadorContrasena.coincide(CONTRASENA_PLANA, CONTRASENA_HASH)).willReturn(true);
        given(proveedorToken.generarAcceso(usuarioActivo))
                .willReturn(new TokenGenerado("token-acceso", EXPIRA_ACCESO));
        given(proveedorToken.generarRefresco(usuarioActivo))
                .willReturn(new TokenGenerado("token-refresco", EXPIRA_REFRESCO));

        var resultado = servicio.iniciarSesion(new CredencialesComando(NOMBRE_USUARIO, CONTRASENA_PLANA));

        assertThat(resultado.tokenAcceso()).isEqualTo("token-acceso");
        assertThat(resultado.tokenRefresco()).isEqualTo("token-refresco");
        assertThat(resultado.expiraEn()).isEqualTo(EXPIRA_ACCESO);
        assertThat(resultado.usuario()).isEqualTo(usuarioActivo);
    }

    @Test
    @DisplayName("iniciarSesion lanza CredencialesInvalidas cuando el usuario no existe")
    void iniciarSesionFallaSiUsuarioNoExiste() {
        given(repositorioUsuario.obtenerPorNombreUsuario(NOMBRE_USUARIO)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.iniciarSesion(new CredencialesComando(NOMBRE_USUARIO, CONTRASENA_PLANA)))
                .isInstanceOf(CredencialesInvalidas.class);
        verify(proveedorToken, never()).generarAcceso(any());
    }

    @Test
    @DisplayName("iniciarSesion lanza CredencialesInvalidas cuando la contraseña no coincide")
    void iniciarSesionFallaSiContrasenaNoCoincide() {
        given(repositorioUsuario.obtenerPorNombreUsuario(NOMBRE_USUARIO)).willReturn(Optional.of(usuarioActivo));
        given(codificadorContrasena.coincide(CONTRASENA_PLANA, CONTRASENA_HASH)).willReturn(false);

        assertThatThrownBy(() -> servicio.iniciarSesion(new CredencialesComando(NOMBRE_USUARIO, CONTRASENA_PLANA)))
                .isInstanceOf(CredencialesInvalidas.class);
        verify(proveedorToken, never()).generarAcceso(any());
    }

    @Test
    @DisplayName("iniciarSesion lanza CredencialesInvalidas cuando el usuario no está activo")
    void iniciarSesionFallaSiUsuarioNoActivo() {
        var bloqueado = construirUsuario(EstadoUsuario.BLOQUEADO);
        given(repositorioUsuario.obtenerPorNombreUsuario(NOMBRE_USUARIO)).willReturn(Optional.of(bloqueado));

        assertThatThrownBy(() -> servicio.iniciarSesion(new CredencialesComando(NOMBRE_USUARIO, CONTRASENA_PLANA)))
                .isInstanceOf(CredencialesInvalidas.class);
        verify(codificadorContrasena, never()).coincide(any(), any());
    }

    @Test
    @DisplayName("refrescarToken devuelve nuevos tokens cuando el refresco es válido")
    void refrescarTokenExitoso() {
        var claimsRefresco = new ClaimsToken(
                ID_USUARIO, null, null, ClaimsToken.TIPO_REFRESCO, EXPIRA_REFRESCO
        );
        given(proveedorToken.validar("token-refresco")).willReturn(claimsRefresco);
        given(repositorioUsuario.obtenerPorId(ID_USUARIO)).willReturn(Optional.of(usuarioActivo));
        given(proveedorToken.generarAcceso(usuarioActivo))
                .willReturn(new TokenGenerado("nuevo-acceso", EXPIRA_ACCESO));
        given(proveedorToken.generarRefresco(usuarioActivo))
                .willReturn(new TokenGenerado("nuevo-refresco", EXPIRA_REFRESCO));

        var resultado = servicio.refrescarToken("token-refresco");

        assertThat(resultado.tokenAcceso()).isEqualTo("nuevo-acceso");
        assertThat(resultado.tokenRefresco()).isEqualTo("nuevo-refresco");
        assertThat(resultado.usuario()).isEqualTo(usuarioActivo);
    }

    @Test
    @DisplayName("refrescarToken propaga CredencialesInvalidas cuando el token es inválido")
    void refrescarTokenFallaSiTokenInvalido() {
        given(proveedorToken.validar("token-malo")).willThrow(new CredencialesInvalidas());

        assertThatThrownBy(() -> servicio.refrescarToken("token-malo"))
                .isInstanceOf(CredencialesInvalidas.class);
        verify(repositorioUsuario, never()).obtenerPorId(any());
    }

    @Test
    @DisplayName("refrescarToken lanza CredencialesInvalidas si el tipo no es refresco")
    void refrescarTokenFallaSiTipoNoEsRefresco() {
        var claimsAcceso = new ClaimsToken(
                ID_USUARIO, NOMBRE_USUARIO, Rol.USUARIO, ClaimsToken.TIPO_ACCESO, EXPIRA_ACCESO
        );
        given(proveedorToken.validar("token-acceso")).willReturn(claimsAcceso);

        assertThatThrownBy(() -> servicio.refrescarToken("token-acceso"))
                .isInstanceOf(CredencialesInvalidas.class);
        verify(repositorioUsuario, never()).obtenerPorId(any());
    }

    @Test
    @DisplayName("refrescarToken lanza CredencialesInvalidas cuando el usuario ya no está activo")
    void refrescarTokenFallaSiUsuarioYaNoEstaActivo() {
        var claimsRefresco = new ClaimsToken(
                ID_USUARIO, null, null, ClaimsToken.TIPO_REFRESCO, EXPIRA_REFRESCO
        );
        given(proveedorToken.validar("token-refresco")).willReturn(claimsRefresco);
        given(repositorioUsuario.obtenerPorId(ID_USUARIO))
                .willReturn(Optional.of(construirUsuario(EstadoUsuario.INACTIVO)));

        assertThatThrownBy(() -> servicio.refrescarToken("token-refresco"))
                .isInstanceOf(CredencialesInvalidas.class);
        verify(proveedorToken, never()).generarAcceso(any());
    }

    @Test
    @DisplayName("refrescarToken lanza CredencialesInvalidas cuando el usuario referenciado no existe")
    void refrescarTokenFallaSiUsuarioNoExiste() {
        var claimsRefresco = new ClaimsToken(
                ID_USUARIO, null, null, ClaimsToken.TIPO_REFRESCO, EXPIRA_REFRESCO
        );
        given(proveedorToken.validar("token-refresco")).willReturn(claimsRefresco);
        given(repositorioUsuario.obtenerPorId(ID_USUARIO)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.refrescarToken("token-refresco"))
                .isInstanceOf(CredencialesInvalidas.class);
    }

    private Usuario construirUsuario(EstadoUsuario estado) {
        var ahora = Instant.parse("2026-01-01T00:00:00Z");
        return new Usuario(
                ID_USUARIO,
                NOMBRE_USUARIO,
                CorreoElectronico.desTexto("diana@correo.com"),
                CONTRASENA_HASH,
                Rol.USUARIO,
                estado,
                ahora,
                ahora
        );
    }
}
