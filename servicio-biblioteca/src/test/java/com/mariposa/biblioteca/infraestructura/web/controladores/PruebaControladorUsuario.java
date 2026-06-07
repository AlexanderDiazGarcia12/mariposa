package com.mariposa.biblioteca.infraestructura.web.controladores;

import com.mariposa.biblioteca.dominio.excepciones.UsuarioNoEncontrado;
import com.mariposa.biblioteca.dominio.excepciones.UsuarioYaExiste;
import com.mariposa.biblioteca.dominio.modelo.EstadoUsuario;
import com.mariposa.biblioteca.dominio.modelo.Rol;
import com.mariposa.biblioteca.dominio.modelo.Usuario;
import com.mariposa.biblioteca.dominio.modelo.consultas.PaginaResultado;
import com.mariposa.biblioteca.dominio.modelo.consultas.Paginacion;
import com.mariposa.biblioteca.dominio.modelo.valor.CorreoElectronico;
import com.mariposa.biblioteca.dominio.puertos.entrada.GestionarUsuariosCasoUso;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.RegistrarUsuarioComando;
import com.mariposa.biblioteca.dominio.puertos.salida.ProveedorToken;
import com.mariposa.biblioteca.infraestructura.seguridad.ConfiguracionSeguridad;
import com.mariposa.biblioteca.infraestructura.seguridad.EscritorRespuestaProblema;
import com.mariposa.biblioteca.infraestructura.seguridad.FiltroAutenticacionInterna;
import com.mariposa.biblioteca.infraestructura.seguridad.FiltroAutenticacionJwt;
import com.mariposa.biblioteca.infraestructura.seguridad.FiltroLimiteTasaInicioSesion;
import com.mariposa.biblioteca.infraestructura.seguridad.ManejadorAccesoDenegadoJwt;
import com.mariposa.biblioteca.infraestructura.seguridad.PuntoEntradaAutenticacionJwt;
import com.mariposa.biblioteca.infraestructura.seguridad.RegistroLimitadoresTasa;
import com.mariposa.biblioteca.infraestructura.web.manejadores.ManejadorGlobalExcepciones;
import com.mariposa.biblioteca.infraestructura.web.mapeadores.MapeadorWebUsuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.mariposa.biblioteca.infraestructura.web.controladores.AuxiliarAutenticacionPrueba.autenticadoComo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ControladorUsuario.class)
@Import({
        ConfiguracionSeguridad.class,
        FiltroAutenticacionJwt.class,
        FiltroAutenticacionInterna.class,
        FiltroLimiteTasaInicioSesion.class,
        RegistroLimitadoresTasa.class,
        PuntoEntradaAutenticacionJwt.class,
        ManejadorAccesoDenegadoJwt.class,
        EscritorRespuestaProblema.class,
        ManejadorGlobalExcepciones.class,
        MapeadorWebUsuario.class
})
@TestPropertySource(properties = {
        "seguridad.jwt.clave-secreta=clave-de-prueba-de-minimo-32-bytes-para-HS256-tests-mariposa",
        "seguridad.jwt.duracion-acceso-minutos=15",
        "seguridad.jwt.duracion-refresco-dias=7",
        "seguridad.jwt.emisor=mariposa-biblioteca",
        "seguridad.interno.secreto=secreto-de-prueba-minimo-16-caracteres",
        "seguridad.limite-tasa.inicio-sesion.habilitado=false",
        "seguridad.limite-tasa.inicio-sesion.capacidad=1000",
        "seguridad.limite-tasa.inicio-sesion.ventana-segundos=60"
})
class PruebaControladorUsuario {

    private static final UUID ID_USUARIO = UUID.fromString("aaaaaaaa-1111-2222-3333-cccccccccccc");
    private static final UUID ID_OTRO = UUID.fromString("bbbbbbbb-2222-3333-4444-dddddddddddd");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GestionarUsuariosCasoUso gestionarUsuariosCasoUso;

    @MockitoBean
    private ProveedorToken proveedorToken;

    @Test
    @DisplayName("POST público con datos válidos crea usuario (201) y devuelve Location")
    void registrarExitoso() throws Exception {
        given(gestionarUsuariosCasoUso.registrar(any(RegistrarUsuarioComando.class)))
                .willReturn(construirUsuario());

        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombreUsuario": "diana.lopez",
                                  "correoElectronico": "diana@correo.com",
                                  "contrasena": "Contrasena.Segura.123",
                                  "rol": "USUARIO"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(ID_USUARIO.toString()))
                .andExpect(jsonPath("$.nombreUsuario").value("diana.lopez"));
    }

    @Test
    @DisplayName("POST con datos inválidos devuelve 400")
    void registrarConDatosInvalidosDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreUsuario":"x","correoElectronico":"no-es-correo","contrasena":"123","rol":"USUARIO"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("validacion"));
    }

    @Test
    @DisplayName("POST con nombre duplicado devuelve 409")
    void registrarConNombreDuplicadoDevuelve409() throws Exception {
        given(gestionarUsuariosCasoUso.registrar(any(RegistrarUsuarioComando.class)))
                .willThrow(new UsuarioYaExiste("diana.lopez"));

        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombreUsuario": "diana.lopez",
                                  "correoElectronico": "diana@correo.com",
                                  "contrasena": "Contrasena.Segura.123",
                                  "rol": "USUARIO"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("usuario-ya-existe"));
    }

    @Test
    @DisplayName("GET /{id} sin autenticación devuelve 401")
    void obtenerPorIdSinAutenticacionDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/{id}", ID_USUARIO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /{id} autenticado como propio usuario devuelve 200")
    void obtenerPorIdAutenticadoComoPropioUsuario() throws Exception {
        given(gestionarUsuariosCasoUso.obtenerPorId(ID_USUARIO)).willReturn(construirUsuario());

        mockMvc.perform(get("/api/v1/usuarios/{id}", ID_USUARIO)
                        .with(autenticadoComo(ID_USUARIO, Rol.USUARIO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID_USUARIO.toString()));
    }

    @Test
    @DisplayName("GET /{id} autenticado como otro usuario sin rol ADMIN devuelve 403")
    void obtenerPorIdConOtroUsuarioDevuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/{id}", ID_USUARIO)
                        .with(autenticadoComo(ID_OTRO, Rol.USUARIO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /{id} no encontrado devuelve 404 (admin)")
    void obtenerPorIdNoEncontradoDevuelve404() throws Exception {
        given(gestionarUsuariosCasoUso.obtenerPorId(ID_USUARIO)).willThrow(new UsuarioNoEncontrado(ID_USUARIO));

        mockMvc.perform(get("/api/v1/usuarios/{id}", ID_USUARIO)
                        .with(autenticadoComo(ID_OTRO, Rol.ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE solo ADMIN devuelve 204")
    void desactivarAdminDevuelve204() throws Exception {
        given(gestionarUsuariosCasoUso.desactivar(ID_USUARIO)).willReturn(construirUsuario());

        mockMvc.perform(delete("/api/v1/usuarios/{id}", ID_USUARIO)
                        .with(autenticadoComo(ID_OTRO, Rol.ADMIN)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE como USUARIO devuelve 403")
    void desactivarComoUsuarioDevuelve403() throws Exception {
        mockMvc.perform(delete("/api/v1/usuarios/{id}", ID_USUARIO)
                        .with(autenticadoComo(ID_OTRO, Rol.USUARIO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET / con rol ADMIN devuelve página con usuarios")
    void listarComoAdminDevuelvePagina() throws Exception {
        var paginacion = new Paginacion(0, 20);
        var pagina = PaginaResultado.de(List.of(construirUsuario()), paginacion, 1L);
        given(gestionarUsuariosCasoUso.listar(paginacion)).willReturn(pagina);

        mockMvc.perform(get("/api/v1/usuarios")
                        .with(autenticadoComo(ID_OTRO, Rol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elementos.length()").value(1))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    private Usuario construirUsuario() {
        var ahora = Instant.parse("2026-01-01T00:00:00Z");
        return new Usuario(
                ID_USUARIO,
                "diana.lopez",
                CorreoElectronico.desTexto("diana@correo.com"),
                "$2a$10$hashbcryptficticio0123456789ABCDEFGHIJKLMNOPQRSTUV",
                Rol.USUARIO,
                EstadoUsuario.ACTIVO,
                ahora,
                ahora
        );
    }
}
