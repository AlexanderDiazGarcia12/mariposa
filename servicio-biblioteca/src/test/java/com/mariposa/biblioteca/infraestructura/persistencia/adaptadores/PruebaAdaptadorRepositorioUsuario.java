package com.mariposa.biblioteca.infraestructura.persistencia.adaptadores;

import com.mariposa.biblioteca.dominio.modelo.Rol;
import com.mariposa.biblioteca.dominio.modelo.Usuario;
import com.mariposa.biblioteca.dominio.modelo.consultas.Paginacion;
import com.mariposa.biblioteca.dominio.modelo.valor.CorreoElectronico;
import com.mariposa.biblioteca.infraestructura.persistencia.BasePruebaPersistencia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PruebaAdaptadorRepositorioUsuario extends BasePruebaPersistencia {

    private static final String CONTRASENA_ENCRIPTADA = "$2a$10$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRS";

    @Autowired
    private AdaptadorRepositorioUsuario adaptador;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void limpiarBaseDatos() {
        entityManager.getEntityManager().createQuery("delete from EntidadUsuario").executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("guardar persiste el usuario y obtenerPorId lo recupera con todos los campos")
    void guardarYObtenerPorId() {
        var usuario = nuevoUsuario("alexdiaz", "alex@mariposa.com", Rol.USUARIO);

        var guardado = adaptador.guardar(usuario);
        flushYLimpiar();

        var recuperado = adaptador.obtenerPorId(guardado.id());
        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().nombreUsuario()).isEqualTo("alexdiaz");
        assertThat(recuperado.get().correoElectronico().valor()).isEqualTo("alex@mariposa.com");
        assertThat(recuperado.get().rol()).isEqualTo(Rol.USUARIO);
        assertThat(recuperado.get().estaActivo()).isTrue();
    }

    @Test
    @DisplayName("obtenerPorNombreUsuario devuelve el usuario buscado")
    void obtenerPorNombreUsuario() {
        adaptador.guardar(nuevoUsuario("administrador", "admin@mariposa.com", Rol.ADMIN));
        flushYLimpiar();

        var recuperado = adaptador.obtenerPorNombreUsuario("administrador");

        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().rol()).isEqualTo(Rol.ADMIN);
    }

    @Test
    @DisplayName("obtenerPorNombreUsuario devuelve vacio si no existe")
    void obtenerPorNombreUsuarioInexistente() {
        var recuperado = adaptador.obtenerPorNombreUsuario("desconocido");

        assertThat(recuperado).isEmpty();
    }

    @Test
    @DisplayName("obtenerPorCorreo devuelve el usuario buscado por su direccion de correo")
    void obtenerPorCorreo() {
        adaptador.guardar(nuevoUsuario("alexdiaz", "alex@mariposa.com", Rol.USUARIO));
        flushYLimpiar();

        var recuperado = adaptador.obtenerPorCorreo(new CorreoElectronico("alex@mariposa.com"));

        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().nombreUsuario()).isEqualTo("alexdiaz");
    }

    @Test
    @DisplayName("existePorNombreUsuario devuelve true cuando existe y false cuando no")
    void existePorNombreUsuario() {
        adaptador.guardar(nuevoUsuario("alexdiaz", "alex@mariposa.com", Rol.USUARIO));
        flushYLimpiar();

        assertThat(adaptador.existePorNombreUsuario("alexdiaz")).isTrue();
        assertThat(adaptador.existePorNombreUsuario("otro")).isFalse();
    }

    @Test
    @DisplayName("existePorCorreo devuelve true cuando existe y false cuando no")
    void existePorCorreo() {
        adaptador.guardar(nuevoUsuario("alexdiaz", "alex@mariposa.com", Rol.USUARIO));
        flushYLimpiar();

        assertThat(adaptador.existePorCorreo(new CorreoElectronico("alex@mariposa.com"))).isTrue();
        assertThat(adaptador.existePorCorreo(new CorreoElectronico("otro@mariposa.com"))).isFalse();
    }

    @Test
    @DisplayName("eliminar borra el usuario por id")
    void eliminarPorId() {
        var guardado = adaptador.guardar(nuevoUsuario("temporal", "temp@mariposa.com", Rol.USUARIO));
        flushYLimpiar();

        adaptador.eliminar(guardado.id());
        flushYLimpiar();

        assertThat(adaptador.obtenerPorId(guardado.id())).isEmpty();
    }

    @Test
    @DisplayName("listar devuelve una pagina con los usuarios ordenados por nombre")
    void listarPaginado() {
        adaptador.guardar(nuevoUsuario("zoraida", "zoraida@mariposa.com", Rol.USUARIO));
        adaptador.guardar(nuevoUsuario("alexdiaz", "alex@mariposa.com", Rol.USUARIO));
        adaptador.guardar(nuevoUsuario("maria", "maria@mariposa.com", Rol.USUARIO));
        flushYLimpiar();

        var resultado = adaptador.listar(new Paginacion(0, 10));

        assertThat(resultado.elementos()).hasSize(3);
        assertThat(resultado.elementos())
                .extracting(Usuario::nombreUsuario)
                .containsExactly("alexdiaz", "maria", "zoraida");
        assertThat(resultado.totalElementos()).isEqualTo(3);
    }

    @Test
    @DisplayName("listar respeta el tamano de pagina")
    void listarConTamanoLimitado() {
        adaptador.guardar(nuevoUsuario("alexdiaz", "alex@mariposa.com", Rol.USUARIO));
        adaptador.guardar(nuevoUsuario("beatriz", "beatriz@mariposa.com", Rol.USUARIO));
        adaptador.guardar(nuevoUsuario("carlos", "carlos@mariposa.com", Rol.USUARIO));
        flushYLimpiar();

        var resultado = adaptador.listar(new Paginacion(0, 2));

        assertThat(resultado.elementos()).hasSize(2);
        assertThat(resultado.totalElementos()).isEqualTo(3);
        assertThat(resultado.totalPaginas()).isEqualTo(2);
    }

    private Usuario nuevoUsuario(String nombre, String correo, Rol rol) {
        return Usuario.nuevo(
                UUID.randomUUID(),
                nombre,
                new CorreoElectronico(correo),
                CONTRASENA_ENCRIPTADA,
                rol
        );
    }

    private void flushYLimpiar() {
        entityManager.flush();
        entityManager.clear();
    }
}
