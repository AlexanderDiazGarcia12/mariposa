package com.mariposa.biblioteca.aplicacion.servicios;

import com.mariposa.biblioteca.dominio.excepciones.UsuarioNoEncontrado;
import com.mariposa.biblioteca.dominio.excepciones.UsuarioYaExiste;
import com.mariposa.biblioteca.dominio.modelo.EstadoUsuario;
import com.mariposa.biblioteca.dominio.modelo.Rol;
import com.mariposa.biblioteca.dominio.modelo.Usuario;
import com.mariposa.biblioteca.dominio.modelo.consultas.PaginaResultado;
import com.mariposa.biblioteca.dominio.modelo.consultas.Paginacion;
import com.mariposa.biblioteca.dominio.modelo.valor.CorreoElectronico;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.ActualizarUsuarioComando;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.RegistrarUsuarioComando;
import com.mariposa.biblioteca.dominio.puertos.salida.CodificadorContrasena;
import com.mariposa.biblioteca.dominio.puertos.salida.RepositorioUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PruebaServicioGestionUsuarios {

    private static final UUID ID_USUARIO = UUID.fromString("aaaaaaaa-1111-2222-3333-bbbbbbbbbbbb");
    private static final String NOMBRE_USUARIO = "diana.lopez";
    private static final CorreoElectronico CORREO = CorreoElectronico.desTexto("diana@correo.com");
    private static final String CONTRASENA_PLANA = "Contrasena.Segura.123";
    private static final String CONTRASENA_HASH = "$2a$10$hashbcryptficticio0123456789ABCDEFGHIJKLMNOPQRSTUV";

    @Mock
    private RepositorioUsuario repositorioUsuario;
    @Mock
    private CodificadorContrasena codificadorContrasena;

    @InjectMocks
    private ServicioGestionUsuarios servicio;

    private Usuario usuarioExistente;

    @BeforeEach
    void inicializar() {
        var ahora = Instant.parse("2026-01-01T00:00:00Z");
        usuarioExistente = new Usuario(
                ID_USUARIO,
                NOMBRE_USUARIO,
                CORREO,
                CONTRASENA_HASH,
                Rol.USUARIO,
                EstadoUsuario.ACTIVO,
                ahora,
                ahora
        );
    }

    @Test
    @DisplayName("registrar codifica la contraseña y guarda el usuario")
    void registrarExitoso() {
        given(repositorioUsuario.existePorNombreUsuario(NOMBRE_USUARIO)).willReturn(false);
        given(repositorioUsuario.existePorCorreo(CORREO)).willReturn(false);
        given(codificadorContrasena.codificar(CONTRASENA_PLANA)).willReturn(CONTRASENA_HASH);
        given(repositorioUsuario.guardar(any(Usuario.class))).willAnswer(invocacion -> invocacion.getArgument(0));

        var comando = new RegistrarUsuarioComando(NOMBRE_USUARIO, CORREO, CONTRASENA_PLANA, Rol.USUARIO);
        var creado = servicio.registrar(comando);

        assertThat(creado.nombreUsuario()).isEqualTo(NOMBRE_USUARIO);
        assertThat(creado.contrasenaEncriptada()).isEqualTo(CONTRASENA_HASH);
        assertThat(creado.estado()).isEqualTo(EstadoUsuario.ACTIVO);
        verify(codificadorContrasena).codificar(CONTRASENA_PLANA);
    }

    @Test
    @DisplayName("registrar lanza UsuarioYaExiste cuando el nombre está duplicado")
    void registrarFallaSiNombreDuplicado() {
        given(repositorioUsuario.existePorNombreUsuario(NOMBRE_USUARIO)).willReturn(true);

        var comando = new RegistrarUsuarioComando(NOMBRE_USUARIO, CORREO, CONTRASENA_PLANA, Rol.USUARIO);
        assertThatThrownBy(() -> servicio.registrar(comando))
                .isInstanceOf(UsuarioYaExiste.class);
        verify(repositorioUsuario, never()).guardar(any());
    }

    @Test
    @DisplayName("registrar lanza UsuarioYaExiste cuando el correo está duplicado")
    void registrarFallaSiCorreoDuplicado() {
        given(repositorioUsuario.existePorNombreUsuario(NOMBRE_USUARIO)).willReturn(false);
        given(repositorioUsuario.existePorCorreo(CORREO)).willReturn(true);

        var comando = new RegistrarUsuarioComando(NOMBRE_USUARIO, CORREO, CONTRASENA_PLANA, Rol.USUARIO);
        assertThatThrownBy(() -> servicio.registrar(comando))
                .isInstanceOf(UsuarioYaExiste.class);
        verify(repositorioUsuario, never()).guardar(any());
    }

    @Test
    @DisplayName("actualizar aplica solo el correo cuando es el único cambio")
    void actualizarSoloCorreo() {
        given(repositorioUsuario.obtenerPorId(ID_USUARIO)).willReturn(Optional.of(usuarioExistente));
        given(repositorioUsuario.guardar(any(Usuario.class))).willAnswer(invocacion -> invocacion.getArgument(0));
        var nuevoCorreo = CorreoElectronico.desTexto("nuevo@correo.com");
        var comando = new ActualizarUsuarioComando(
                ID_USUARIO, Optional.of(nuevoCorreo), Optional.empty(), Optional.empty()
        );

        var actualizado = servicio.actualizar(comando);

        assertThat(actualizado.correoElectronico()).isEqualTo(nuevoCorreo);
        assertThat(actualizado.rol()).isEqualTo(Rol.USUARIO);
        assertThat(actualizado.contrasenaEncriptada()).isEqualTo(CONTRASENA_HASH);
        verify(codificadorContrasena, never()).codificar(any());
    }

    @Test
    @DisplayName("actualizar aplica solo el rol cuando es el único cambio")
    void actualizarSoloRol() {
        given(repositorioUsuario.obtenerPorId(ID_USUARIO)).willReturn(Optional.of(usuarioExistente));
        given(repositorioUsuario.guardar(any(Usuario.class))).willAnswer(invocacion -> invocacion.getArgument(0));
        var comando = new ActualizarUsuarioComando(
                ID_USUARIO, Optional.empty(), Optional.of(Rol.ADMIN), Optional.empty()
        );

        var actualizado = servicio.actualizar(comando);

        assertThat(actualizado.rol()).isEqualTo(Rol.ADMIN);
        assertThat(actualizado.correoElectronico()).isEqualTo(CORREO);
    }

    @Test
    @DisplayName("actualizar aplica solo la contraseña codificándola")
    void actualizarSoloContrasena() {
        given(repositorioUsuario.obtenerPorId(ID_USUARIO)).willReturn(Optional.of(usuarioExistente));
        given(repositorioUsuario.guardar(any(Usuario.class))).willAnswer(invocacion -> invocacion.getArgument(0));
        given(codificadorContrasena.codificar("nueva-contrasena-2026")).willReturn("$2a$10$nuevoHashCodificadoLargoParaTestsValidos1234567890ABCDE");
        var comando = new ActualizarUsuarioComando(
                ID_USUARIO, Optional.empty(), Optional.empty(), Optional.of("nueva-contrasena-2026")
        );

        var actualizado = servicio.actualizar(comando);

        assertThat(actualizado.contrasenaEncriptada()).startsWith("$2a$10$nuevoHash");
        verify(codificadorContrasena).codificar("nueva-contrasena-2026");
    }

    @Test
    @DisplayName("actualizar lanza UsuarioNoEncontrado si el id no existe")
    void actualizarFallaSiNoExiste() {
        given(repositorioUsuario.obtenerPorId(ID_USUARIO)).willReturn(Optional.empty());
        var comando = new ActualizarUsuarioComando(
                ID_USUARIO, Optional.empty(), Optional.of(Rol.ADMIN), Optional.empty()
        );

        assertThatThrownBy(() -> servicio.actualizar(comando)).isInstanceOf(UsuarioNoEncontrado.class);
        verify(repositorioUsuario, never()).guardar(any());
    }

    @Test
    @DisplayName("desactivar marca al usuario como INACTIVO y guarda")
    void desactivarExitoso() {
        given(repositorioUsuario.obtenerPorId(ID_USUARIO)).willReturn(Optional.of(usuarioExistente));
        given(repositorioUsuario.guardar(any(Usuario.class))).willAnswer(invocacion -> invocacion.getArgument(0));

        var resultado = servicio.desactivar(ID_USUARIO);

        var captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repositorioUsuario).guardar(captor.capture());
        assertThat(captor.getValue().estado()).isEqualTo(EstadoUsuario.INACTIVO);
        assertThat(resultado.estado()).isEqualTo(EstadoUsuario.INACTIVO);
    }

    @Test
    @DisplayName("desactivar lanza UsuarioNoEncontrado si el id no existe")
    void desactivarFallaSiNoExiste() {
        given(repositorioUsuario.obtenerPorId(ID_USUARIO)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.desactivar(ID_USUARIO)).isInstanceOf(UsuarioNoEncontrado.class);
    }

    @Test
    @DisplayName("obtenerPorId devuelve usuario cuando existe")
    void obtenerPorIdExitoso() {
        given(repositorioUsuario.obtenerPorId(ID_USUARIO)).willReturn(Optional.of(usuarioExistente));

        assertThat(servicio.obtenerPorId(ID_USUARIO)).isEqualTo(usuarioExistente);
    }

    @Test
    @DisplayName("obtenerPorId lanza UsuarioNoEncontrado cuando no existe")
    void obtenerPorIdFallaSiNoExiste() {
        given(repositorioUsuario.obtenerPorId(ID_USUARIO)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerPorId(ID_USUARIO)).isInstanceOf(UsuarioNoEncontrado.class);
    }

    @Test
    @DisplayName("listar delega al repositorio con la paginación recibida")
    void listarDelegaAlRepositorio() {
        var paginacion = new Paginacion(0, 10);
        var paginaEsperada = PaginaResultado.de(List.of(usuarioExistente), paginacion, 1L);
        given(repositorioUsuario.listar(paginacion)).willReturn(paginaEsperada);

        assertThat(servicio.listar(paginacion)).isEqualTo(paginaEsperada);
    }
}
