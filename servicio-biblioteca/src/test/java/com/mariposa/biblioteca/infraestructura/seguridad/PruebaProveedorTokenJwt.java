package com.mariposa.biblioteca.infraestructura.seguridad;

import com.mariposa.biblioteca.dominio.excepciones.CredencialesInvalidas;
import com.mariposa.biblioteca.dominio.modelo.Rol;
import com.mariposa.biblioteca.dominio.modelo.Usuario;
import com.mariposa.biblioteca.dominio.modelo.valor.CorreoElectronico;
import com.mariposa.biblioteca.dominio.puertos.salida.ProveedorToken.ClaimsToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PruebaProveedorTokenJwt {

    private static final String CLAVE_VALIDA = "una-clave-secreta-de-prueba-para-firmar-tokens-HS256-mariposa";
    private static final String CLAVE_OTRA = "una-clave-secreta-DISTINTA-para-tokens-HS256-prueba-mariposa";
    private static final String EMISOR_VALIDO = "mariposa-biblioteca";
    private static final Instant INSTANTE_FIJO = Instant.parse("2026-06-05T10:00:00Z");

    @Test
    @DisplayName("generarAcceso produce un token válido que pasa validar con tipo acceso")
    void generarAccesoProduceTokenValidoConTipoAcceso() {
        var proveedor = construirProveedor(propiedadesEstandar(), relojFijo(INSTANTE_FIJO));
        var usuario = construirUsuarioActivo();

        var generado = proveedor.generarAcceso(usuario);
        var claims = proveedor.validar(generado.token());

        assertThat(generado.token()).isNotBlank();
        assertThat(claims.idUsuario()).isEqualTo(usuario.id());
        assertThat(claims.nombreUsuario()).isEqualTo(usuario.nombreUsuario());
        assertThat(claims.rol()).isEqualTo(Rol.USUARIO);
        assertThat(claims.esAcceso()).isTrue();
        assertThat(claims.expiraEn()).isEqualTo(INSTANTE_FIJO.plusSeconds(15 * 60L));
    }

    @Test
    @DisplayName("generarRefresco produce un token válido tipo refresco con expiración correcta")
    void generarRefrescoProduceTokenValidoConTipoRefresco() {
        var proveedor = construirProveedor(propiedadesEstandar(), relojFijo(INSTANTE_FIJO));
        var usuario = construirUsuarioActivo();

        var generado = proveedor.generarRefresco(usuario);
        var claims = proveedor.validar(generado.token());

        assertThat(claims.esRefresco()).isTrue();
        assertThat(claims.idUsuario()).isEqualTo(usuario.id());
        assertThat(claims.expiraEn()).isEqualTo(INSTANTE_FIJO.plus(java.time.Duration.ofDays(7)));
    }

    @Test
    @DisplayName("validar lanza CredencialesInvalidas cuando el token está expirado")
    void validarDetectaTokenExpirado() {
        var reloj = new RelojMutable(INSTANTE_FIJO);
        var proveedor = construirProveedor(propiedadesCortas(), reloj);
        var token = proveedor.generarAcceso(construirUsuarioActivo()).token();

        reloj.avanzar(java.time.Duration.ofMinutes(10));

        assertThatThrownBy(() -> proveedor.validar(token))
                .isInstanceOf(CredencialesInvalidas.class);
    }

    @Test
    @DisplayName("validar lanza CredencialesInvalidas cuando la firma es incorrecta")
    void validarDetectaFirmaIncorrecta() {
        var emisor = construirProveedor(propiedadesConClave(CLAVE_VALIDA), relojFijo(INSTANTE_FIJO));
        var verificador = construirProveedor(propiedadesConClave(CLAVE_OTRA), relojFijo(INSTANTE_FIJO));
        var token = emisor.generarAcceso(construirUsuarioActivo()).token();

        assertThatThrownBy(() -> verificador.validar(token))
                .isInstanceOf(CredencialesInvalidas.class);
    }

    @Test
    @DisplayName("validar lanza CredencialesInvalidas cuando el emisor no coincide")
    void validarDetectaEmisorIncorrecto() {
        var emisor = construirProveedor(propiedadesConEmisor("otro-emisor"), relojFijo(INSTANTE_FIJO));
        var verificador = construirProveedor(propiedadesEstandar(), relojFijo(INSTANTE_FIJO));
        var token = emisor.generarAcceso(construirUsuarioActivo()).token();

        assertThatThrownBy(() -> verificador.validar(token))
                .isInstanceOf(CredencialesInvalidas.class);
    }

    @Test
    @DisplayName("validar lanza CredencialesInvalidas cuando el token es nulo o vacío")
    void validarDetectaTokenNuloOVacio() {
        var proveedor = construirProveedor(propiedadesEstandar(), relojFijo(INSTANTE_FIJO));

        assertThatThrownBy(() -> proveedor.validar(null)).isInstanceOf(CredencialesInvalidas.class);
        assertThatThrownBy(() -> proveedor.validar("   ")).isInstanceOf(CredencialesInvalidas.class);
        assertThatThrownBy(() -> proveedor.validar("no-es-un-jwt")).isInstanceOf(CredencialesInvalidas.class);
    }

    @Test
    @DisplayName("ClaimsToken reconstruye id, nombre, rol y tipo correctamente")
    void claimsTokenReconstruyeAtributosBasicos() {
        var proveedor = construirProveedor(propiedadesEstandar(), relojFijo(INSTANTE_FIJO));
        var usuario = construirUsuarioActivo();

        var claimsAcceso = proveedor.validar(proveedor.generarAcceso(usuario).token());
        var claimsRefresco = proveedor.validar(proveedor.generarRefresco(usuario).token());

        assertThat(claimsAcceso)
                .extracting(ClaimsToken::idUsuario, ClaimsToken::nombreUsuario, ClaimsToken::rol, ClaimsToken::tipo)
                .containsExactly(usuario.id(), usuario.nombreUsuario(), Rol.USUARIO, ClaimsToken.TIPO_ACCESO);
        assertThat(claimsRefresco.tipo()).isEqualTo(ClaimsToken.TIPO_REFRESCO);
    }

    private ProveedorTokenJwt construirProveedor(PropiedadesJwt propiedades, Clock reloj) {
        return new ProveedorTokenJwt(propiedades, reloj);
    }

    private PropiedadesJwt propiedadesEstandar() {
        return new PropiedadesJwt(CLAVE_VALIDA, 15, 7, EMISOR_VALIDO);
    }

    private PropiedadesJwt propiedadesCortas() {
        return new PropiedadesJwt(CLAVE_VALIDA, 1, 1, EMISOR_VALIDO);
    }

    private PropiedadesJwt propiedadesConClave(String clave) {
        return new PropiedadesJwt(clave, 15, 7, EMISOR_VALIDO);
    }

    private PropiedadesJwt propiedadesConEmisor(String emisor) {
        return new PropiedadesJwt(CLAVE_VALIDA, 15, 7, emisor);
    }

    private Clock relojFijo(Instant instante) {
        return Clock.fixed(instante, ZoneOffset.UTC);
    }

    private Usuario construirUsuarioActivo() {
        return Usuario.nuevo(
                UUID.fromString("11111111-2222-3333-4444-555555555555"),
                "luna.perez",
                CorreoElectronico.desTexto("luna@correo.com"),
                "$2a$10$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKL",
                Rol.USUARIO
        );
    }

    private static final class RelojMutable extends Clock {

        private Instant ahora;

        private RelojMutable(Instant inicial) {
            this.ahora = inicial;
        }

        void avanzar(java.time.Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }
}
