package com.mariposa.biblioteca.infraestructura.web.controladores;

import com.mariposa.biblioteca.dominio.excepciones.LibroNoEncontrado;
import com.mariposa.biblioteca.dominio.modelo.Genero;
import com.mariposa.biblioteca.dominio.modelo.Libro;
import com.mariposa.biblioteca.dominio.modelo.valor.Isbn;
import com.mariposa.biblioteca.dominio.puertos.entrada.GestionarLibrosCasoUso;
import com.mariposa.biblioteca.dominio.puertos.salida.ProveedorToken;
import com.mariposa.biblioteca.infraestructura.seguridad.ConfiguracionSeguridad;
import com.mariposa.biblioteca.infraestructura.seguridad.EscritorRespuestaProblema;
import com.mariposa.biblioteca.infraestructura.seguridad.FiltroAutenticacionInterna;
import com.mariposa.biblioteca.infraestructura.seguridad.FiltroAutenticacionJwt;
import com.mariposa.biblioteca.infraestructura.seguridad.ManejadorAccesoDenegadoJwt;
import com.mariposa.biblioteca.infraestructura.seguridad.PropiedadesServicioInterno;
import com.mariposa.biblioteca.infraestructura.seguridad.PuntoEntradaAutenticacionJwt;
import com.mariposa.biblioteca.infraestructura.web.manejadores.ManejadorGlobalExcepciones;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ControladorLibroInterno.class)
@Import({
        ConfiguracionSeguridad.class,
        FiltroAutenticacionJwt.class,
        FiltroAutenticacionInterna.class,
        PuntoEntradaAutenticacionJwt.class,
        ManejadorAccesoDenegadoJwt.class,
        EscritorRespuestaProblema.class,
        ManejadorGlobalExcepciones.class
})
@TestPropertySource(properties = {
        "seguridad.jwt.clave-secreta=clave-de-prueba-de-minimo-32-bytes-para-HS256-tests-mariposa",
        "seguridad.jwt.duracion-acceso-minutos=15",
        "seguridad.jwt.duracion-refresco-dias=7",
        "seguridad.jwt.emisor=mariposa-biblioteca",
        "seguridad.interno.secreto=secreto-de-prueba-minimo-16-caracteres"
})
class PruebaControladorLibroInterno {

    private static final String SECRETO_VALIDO = "secreto-de-prueba-minimo-16-caracteres";
    private static final String SECRETO_INVALIDO = "otro-secreto-no-coincide";
    private static final UUID ID_LIBRO = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final Isbn ISBN = Isbn.desTexto("9781491950357");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GestionarLibrosCasoUso gestionarLibrosCasoUso;

    @MockitoBean
    private ProveedorToken proveedorToken;

    @Test
    @DisplayName("GET con header X-Servicio-Interno válido devuelve 200 con datos del libro")
    void obtenerConHeaderValidoDevuelve200() throws Exception {
        given(gestionarLibrosCasoUso.obtenerPorId(ID_LIBRO)).willReturn(construirLibro());

        mockMvc.perform(get("/api/v1/internal/libros/{id}", ID_LIBRO)
                        .header("X-Servicio-Interno", SECRETO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID_LIBRO.toString()))
                .andExpect(jsonPath("$.titulo").value("Clean Code"))
                .andExpect(jsonPath("$.copiasDisponibles").value(7))
                .andExpect(jsonPath("$.copiasTotales").value(10))
                .andExpect(jsonPath("$.isbn").value("9781491950357"));
    }

    @Test
    @DisplayName("GET sin header X-Servicio-Interno devuelve 401")
    void obtenerSinHeaderDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/internal/libros/{id}", ID_LIBRO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET con header X-Servicio-Interno incorrecto devuelve 401")
    void obtenerConHeaderIncorrectoDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/internal/libros/{id}", ID_LIBRO)
                        .header("X-Servicio-Interno", SECRETO_INVALIDO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET libro inexistente con header válido devuelve 404")
    void obtenerLibroInexistenteDevuelve404() throws Exception {
        given(gestionarLibrosCasoUso.obtenerPorId(ID_LIBRO))
                .willThrow(new LibroNoEncontrado(ID_LIBRO));

        mockMvc.perform(get("/api/v1/internal/libros/{id}", ID_LIBRO)
                        .header("X-Servicio-Interno", SECRETO_VALIDO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("libro-no-encontrado"));
    }

    @Test
    @DisplayName("GET con header válido y Bearer JWT también devuelve 200 (cadena interna independiente)")
    void obtenerConHeaderValidoYJwtDevuelve200() throws Exception {
        given(gestionarLibrosCasoUso.obtenerPorId(ID_LIBRO)).willReturn(construirLibro());

        mockMvc.perform(get("/api/v1/internal/libros/{id}", ID_LIBRO)
                        .header("X-Servicio-Interno", SECRETO_VALIDO)
                        .header("Authorization", "Bearer un-token-cualquiera-que-no-importa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID_LIBRO.toString()));
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
                7,
                ahora,
                ahora
        );
    }
}
