package com.mariposa.biblioteca.dominio.modelo;

import com.mariposa.biblioteca.dominio.excepciones.ValorInvalidoDominio;
import com.mariposa.biblioteca.dominio.modelo.valor.CorreoElectronico;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PruebaUsuario {

    private static final UUID ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final CorreoElectronico CORREO = CorreoElectronico.desTexto("usuario@dominio.com");
    private static final String CONTRASENA = "$2a$10$hashCifrado";
    private static final Instant AHORA = Instant.parse("2026-01-15T10:00:00Z");

    private static Usuario usuarioConEstado(EstadoUsuario estado) {
        return new Usuario(
                ID, "usuario", CORREO, CONTRASENA,
                Rol.USUARIO, estado, AHORA, AHORA
        );
    }

    @Nested
    class ConstructorCompacto {

        @Test
        void debeRechazarIdNulo() {
            assertThatThrownBy(() -> new Usuario(
                    null, "usuario", CORREO, CONTRASENA, Rol.USUARIO, EstadoUsuario.ACTIVO, AHORA, AHORA
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("id");
        }

        @Test
        void debeRechazarCorreoNulo() {
            assertThatThrownBy(() -> new Usuario(
                    ID, "usuario", null, CONTRASENA, Rol.USUARIO, EstadoUsuario.ACTIVO, AHORA, AHORA
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("correoElectronico");
        }

        @Test
        void debeRechazarRolNulo() {
            assertThatThrownBy(() -> new Usuario(
                    ID, "usuario", CORREO, CONTRASENA, null, EstadoUsuario.ACTIVO, AHORA, AHORA
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("rol");
        }

        @Test
        void debeRechazarEstadoNulo() {
            assertThatThrownBy(() -> new Usuario(
                    ID, "usuario", CORREO, CONTRASENA, Rol.USUARIO, null, AHORA, AHORA
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("estado");
        }

        @Test
        void debeRechazarFechaCreacionNula() {
            assertThatThrownBy(() -> new Usuario(
                    ID, "usuario", CORREO, CONTRASENA, Rol.USUARIO, EstadoUsuario.ACTIVO, null, AHORA
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("fechaCreacion");
        }

        @Test
        void debeRechazarFechaActualizacionNula() {
            assertThatThrownBy(() -> new Usuario(
                    ID, "usuario", CORREO, CONTRASENA, Rol.USUARIO, EstadoUsuario.ACTIVO, AHORA, null
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("fechaActualizacion");
        }

        @Test
        void debeRechazarNombreUsuarioNulo() {
            assertThatThrownBy(() -> new Usuario(
                    ID, null, CORREO, CONTRASENA, Rol.USUARIO, EstadoUsuario.ACTIVO, AHORA, AHORA
            ))
                    .isInstanceOf(ValorInvalidoDominio.class)
                    .extracting("campo").isEqualTo("nombreUsuario");
        }

        @Test
        void debeRechazarNombreUsuarioEnBlanco() {
            assertThatThrownBy(() -> new Usuario(
                    ID, "   ", CORREO, CONTRASENA, Rol.USUARIO, EstadoUsuario.ACTIVO, AHORA, AHORA
            ))
                    .isInstanceOf(ValorInvalidoDominio.class)
                    .extracting("campo").isEqualTo("nombreUsuario");
        }

        @Test
        void debeRechazarNombreUsuarioDemasiadoCorto() {
            assertThatThrownBy(() -> new Usuario(
                    ID, "ab", CORREO, CONTRASENA, Rol.USUARIO, EstadoUsuario.ACTIVO, AHORA, AHORA
            ))
                    .isInstanceOf(ValorInvalidoDominio.class)
                    .hasMessageContaining("entre 3 y 60");
        }

        @Test
        void debeRechazarNombreUsuarioDemasiadoLargo() {
            var nombreLargo = "x".repeat(61);

            assertThatThrownBy(() -> new Usuario(
                    ID, nombreLargo, CORREO, CONTRASENA, Rol.USUARIO, EstadoUsuario.ACTIVO, AHORA, AHORA
            ))
                    .isInstanceOf(ValorInvalidoDominio.class)
                    .hasMessageContaining("entre 3 y 60");
        }

        @Test
        void debeRechazarContrasenaNula() {
            assertThatThrownBy(() -> new Usuario(
                    ID, "usuario", CORREO, null, Rol.USUARIO, EstadoUsuario.ACTIVO, AHORA, AHORA
            ))
                    .isInstanceOf(ValorInvalidoDominio.class)
                    .extracting("campo").isEqualTo("contrasenaEncriptada");
        }

        @Test
        void debeRechazarContrasenaEnBlanco() {
            assertThatThrownBy(() -> new Usuario(
                    ID, "usuario", CORREO, "  ", Rol.USUARIO, EstadoUsuario.ACTIVO, AHORA, AHORA
            ))
                    .isInstanceOf(ValorInvalidoDominio.class)
                    .extracting("campo").isEqualTo("contrasenaEncriptada");
        }

        @Test
        void debeAceptarParametrosValidos() {
            var usuario = usuarioConEstado(EstadoUsuario.ACTIVO);

            assertThat(usuario.id()).isEqualTo(ID);
            assertThat(usuario.nombreUsuario()).isEqualTo("usuario");
            assertThat(usuario.rol()).isEqualTo(Rol.USUARIO);
        }
    }

    @Nested
    class FactoryNuevo {

        @Test
        void debeCrearUsuarioConEstadoActivo() {
            var usuario = Usuario.nuevo(ID, "usuario", CORREO, CONTRASENA, Rol.USUARIO);

            assertThat(usuario.estado()).isEqualTo(EstadoUsuario.ACTIVO);
            assertThat(usuario.estaActivo()).isTrue();
        }

        @Test
        void debePreservarRolInicial() {
            var usuario = Usuario.nuevo(ID, "admin01", CORREO, CONTRASENA, Rol.ADMIN);

            assertThat(usuario.rol()).isEqualTo(Rol.ADMIN);
        }

        @Test
        void debeCrearUsuarioConFechaCreacionIgualAFechaActualizacion() {
            var usuario = Usuario.nuevo(ID, "usuario", CORREO, CONTRASENA, Rol.USUARIO);

            assertThat(usuario.fechaCreacion()).isEqualTo(usuario.fechaActualizacion());
        }
    }

    @Nested
    class CambiarContrasena {

        @Test
        void debeRechazarContrasenaEnBlanco() {
            var usuario = usuarioConEstado(EstadoUsuario.ACTIVO);

            assertThatThrownBy(() -> usuario.cambiarContrasena(" "))
                    .isInstanceOf(ValorInvalidoDominio.class)
                    .extracting("campo").isEqualTo("contrasenaEncriptada");
        }

        @Test
        void debeRechazarContrasenaNula() {
            var usuario = usuarioConEstado(EstadoUsuario.ACTIVO);

            assertThatThrownBy(() -> usuario.cambiarContrasena(null))
                    .isInstanceOf(ValorInvalidoDominio.class)
                    .extracting("campo").isEqualTo("contrasenaEncriptada");
        }

        @Test
        void debeActualizarContrasenaYFechaActualizacion() {
            var usuario = usuarioConEstado(EstadoUsuario.ACTIVO);

            var modificado = usuario.cambiarContrasena("$2a$10$nuevoHash");

            assertThat(modificado.contrasenaEncriptada()).isEqualTo("$2a$10$nuevoHash");
            assertThat(modificado.fechaActualizacion()).isAfterOrEqualTo(usuario.fechaActualizacion());
            assertThat(modificado.fechaCreacion()).isEqualTo(usuario.fechaCreacion());
        }
    }

    @Nested
    class Desactivar {

        @Test
        void debeCambiarEstadoAInactivo() {
            var usuario = usuarioConEstado(EstadoUsuario.ACTIVO);

            var desactivado = usuario.desactivar();

            assertThat(desactivado.estado()).isEqualTo(EstadoUsuario.INACTIVO);
            assertThat(desactivado.estaActivo()).isFalse();
        }

        @Test
        void debeSerIdempotenteSiYaEstaInactivo() {
            var usuario = usuarioConEstado(EstadoUsuario.INACTIVO);

            var resultado = usuario.desactivar();

            assertThat(resultado).isSameAs(usuario);
        }
    }

    @Nested
    class Activar {

        @Test
        void debeCambiarEstadoAActivo() {
            var usuario = usuarioConEstado(EstadoUsuario.INACTIVO);

            var activado = usuario.activar();

            assertThat(activado.estado()).isEqualTo(EstadoUsuario.ACTIVO);
            assertThat(activado.estaActivo()).isTrue();
        }

        @Test
        void debeSerIdempotenteSiYaEstaActivo() {
            var usuario = usuarioConEstado(EstadoUsuario.ACTIVO);

            var resultado = usuario.activar();

            assertThat(resultado).isSameAs(usuario);
        }
    }

    @Nested
    class Bloquear {

        @Test
        void debeCambiarEstadoABloqueado() {
            var usuario = usuarioConEstado(EstadoUsuario.ACTIVO);

            var bloqueado = usuario.bloquear();

            assertThat(bloqueado.estado()).isEqualTo(EstadoUsuario.BLOQUEADO);
            assertThat(bloqueado.estaActivo()).isFalse();
        }

        @Test
        void debeSerIdempotenteSiYaEstaBloqueado() {
            var usuario = usuarioConEstado(EstadoUsuario.BLOQUEADO);

            var resultado = usuario.bloquear();

            assertThat(resultado).isSameAs(usuario);
        }
    }

    @Nested
    class CambiarRol {

        @Test
        void debeRechazarRolNulo() {
            var usuario = usuarioConEstado(EstadoUsuario.ACTIVO);

            assertThatThrownBy(() -> usuario.cambiarRol(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("nuevoRol");
        }

        @Test
        void debeCambiarRolDeUsuarioAAdmin() {
            var usuario = usuarioConEstado(EstadoUsuario.ACTIVO);

            var ascendido = usuario.cambiarRol(Rol.ADMIN);

            assertThat(ascendido.rol()).isEqualTo(Rol.ADMIN);
            assertThat(ascendido.fechaActualizacion()).isAfterOrEqualTo(usuario.fechaActualizacion());
        }

        @Test
        void debeSerIdempotenteSiElRolEsElMismo() {
            var usuario = usuarioConEstado(EstadoUsuario.ACTIVO);

            var resultado = usuario.cambiarRol(Rol.USUARIO);

            assertThat(resultado).isSameAs(usuario);
        }
    }

    @Nested
    class EstaActivo {

        @Test
        void debeRetornarTrueSiEstadoEsActivo() {
            var usuario = usuarioConEstado(EstadoUsuario.ACTIVO);

            assertThat(usuario.estaActivo()).isTrue();
        }

        @Test
        void debeRetornarFalseSiEstadoEsInactivo() {
            var usuario = usuarioConEstado(EstadoUsuario.INACTIVO);

            assertThat(usuario.estaActivo()).isFalse();
        }

        @Test
        void debeRetornarFalseSiEstadoEsBloqueado() {
            var usuario = usuarioConEstado(EstadoUsuario.BLOQUEADO);

            assertThat(usuario.estaActivo()).isFalse();
        }
    }
}
