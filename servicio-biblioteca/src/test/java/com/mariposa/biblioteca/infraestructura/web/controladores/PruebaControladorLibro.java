package com.mariposa.biblioteca.infraestructura.web.controladores;

import com.mariposa.biblioteca.dominio.excepciones.LibroNoEncontrado;
import com.mariposa.biblioteca.dominio.excepciones.LibroYaExiste;
import com.mariposa.biblioteca.dominio.modelo.Genero;
import com.mariposa.biblioteca.dominio.modelo.Libro;
import com.mariposa.biblioteca.dominio.modelo.Rol;
import com.mariposa.biblioteca.dominio.modelo.consultas.FiltroLibros;
import com.mariposa.biblioteca.dominio.modelo.consultas.PaginaResultado;
import com.mariposa.biblioteca.dominio.modelo.consultas.Paginacion;
import com.mariposa.biblioteca.dominio.modelo.valor.Isbn;
import com.mariposa.biblioteca.dominio.puertos.entrada.GestionarLibrosCasoUso;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.CrearLibroComando;
import com.mariposa.biblioteca.dominio.puertos.salida.ProveedorToken;
import com.mariposa.biblioteca.infraestructura.seguridad.ConfiguracionSeguridad;
import com.mariposa.biblioteca.infraestructura.seguridad.EscritorRespuestaProblema;
import com.mariposa.biblioteca.infraestructura.seguridad.FiltroAutenticacionInterna;
import com.mariposa.biblioteca.infraestructura.seguridad.FiltroAutenticacionJwt;
import com.mariposa.biblioteca.infraestructura.seguridad.ManejadorAccesoDenegadoJwt;
import com.mariposa.biblioteca.infraestructura.seguridad.PuntoEntradaAutenticacionJwt;
import com.mariposa.biblioteca.infraestructura.web.manejadores.ManejadorGlobalExcepciones;
import com.mariposa.biblioteca.infraestructura.web.mapeadores.MapeadorWebLibro;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ControladorLibro.class)
@Import({
        ConfiguracionSeguridad.class,
        FiltroAutenticacionJwt.class,
        FiltroAutenticacionInterna.class,
        PuntoEntradaAutenticacionJwt.class,
        ManejadorAccesoDenegadoJwt.class,
        EscritorRespuestaProblema.class,
        ManejadorGlobalExcepciones.class,
        MapeadorWebLibro.class
})
@TestPropertySource(properties = {
        "seguridad.jwt.clave-secreta=clave-de-prueba-de-minimo-32-bytes-para-HS256-tests-mariposa",
        "seguridad.jwt.duracion-acceso-minutos=15",
        "seguridad.jwt.duracion-refresco-dias=7",
        "seguridad.jwt.emisor=mariposa-biblioteca",
        "seguridad.interno.secreto=secreto-de-prueba-minimo-16-caracteres"
})
class PruebaControladorLibro {

    private static final UUID ID_LIBRO = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID ID_USUARIO = UUID.fromString("aaaaaaaa-1111-2222-3333-cccccccccccc");
    private static final Isbn ISBN = Isbn.desTexto("9781491950357");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GestionarLibrosCasoUso gestionarLibrosCasoUso;

    @MockitoBean
    private ProveedorToken proveedorToken;

    @Test
    @DisplayName("GET / autenticado con filtros devuelve 200 con página")
    void listarConFiltrosDevuelvePagina() throws Exception {
        var paginacion = new Paginacion(0, 20);
        var pagina = PaginaResultado.de(List.of(construirLibro()), paginacion, 1L);
        given(gestionarLibrosCasoUso.listar(any(FiltroLibros.class), any(Paginacion.class))).willReturn(pagina);

        mockMvc.perform(get("/api/v1/libros")
                        .param("autor", "Martin")
                        .param("genero", "TECNICO")
                        .with(autenticadoComo(ID_USUARIO, Rol.USUARIO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elementos.length()").value(1))
                .andExpect(jsonPath("$.elementos[0].isbn").value("9781491950357"));
    }

    @Test
    @DisplayName("GET sin autenticación devuelve 401")
    void listarSinAutenticacionDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/libros"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /{id} no encontrado devuelve 404")
    void obtenerPorIdNoEncontradoDevuelve404() throws Exception {
        given(gestionarLibrosCasoUso.obtenerPorId(ID_LIBRO)).willThrow(new LibroNoEncontrado(ID_LIBRO));

        mockMvc.perform(get("/api/v1/libros/{id}", ID_LIBRO)
                        .with(autenticadoComo(ID_USUARIO, Rol.USUARIO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("libro-no-encontrado"));
    }

    @Test
    @DisplayName("POST sin rol ADMIN devuelve 403")
    void crearComoUsuarioDevuelve403() throws Exception {
        mockMvc.perform(post("/api/v1/libros")
                        .with(autenticadoComo(ID_USUARIO, Rol.USUARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCreacion()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST con ADMIN crea libro y devuelve 201 con Location")
    void crearComoAdminDevuelve201() throws Exception {
        given(gestionarLibrosCasoUso.crear(any(CrearLibroComando.class))).willReturn(construirLibro());

        mockMvc.perform(post("/api/v1/libros")
                        .with(autenticadoComo(ID_USUARIO, Rol.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCreacion()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(ID_LIBRO.toString()))
                .andExpect(jsonPath("$.titulo").value("Clean Code"));
    }

    @Test
    @DisplayName("POST con ISBN duplicado devuelve 409")
    void crearConIsbnDuplicadoDevuelve409() throws Exception {
        given(gestionarLibrosCasoUso.crear(any(CrearLibroComando.class))).willThrow(new LibroYaExiste(ISBN));

        mockMvc.perform(post("/api/v1/libros")
                        .with(autenticadoComo(ID_USUARIO, Rol.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCreacion()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("libro-ya-existe"));
    }

    @Test
    @DisplayName("POST con cuerpo inválido devuelve 400")
    void crearConCuerpoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/libros")
                        .with(autenticadoComo(ID_USUARIO, Rol.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"","autor":"","isbn":"","anioPublicacion":500,"genero":null,"copiasTotales":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("validacion"));
    }

    @Test
    @DisplayName("GET /por-isbn/{isbn} devuelve libro autenticado")
    void obtenerPorIsbnExitoso() throws Exception {
        given(gestionarLibrosCasoUso.obtenerPorIsbn(ISBN)).willReturn(construirLibro());

        mockMvc.perform(get("/api/v1/libros/por-isbn/{isbn}", "9781491950357")
                        .with(autenticadoComo(ID_USUARIO, Rol.USUARIO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value("9781491950357"));
    }

    private Libro construirLibro() {
        var ahora = Instant.parse("2026-01-01T00:00:00Z");
        return new Libro(
                ID_LIBRO,
                "Clean Code",
                "Robert C. Martin",
                ISBN,
                2008,
                Genero.TECNICO,
                10,
                10,
                ahora,
                ahora
        );
    }

    private String cuerpoCreacion() {
        return """
                {
                  "titulo": "Clean Code",
                  "autor": "Robert C. Martin",
                  "isbn": "9781491950357",
                  "anioPublicacion": 2008,
                  "genero": "TECNICO",
                  "copiasTotales": 10
                }
                """;
    }
}
