package com.mariposa.biblioteca.infraestructura.web.controladores;

import com.mariposa.biblioteca.dominio.excepciones.ServicioPrestamosNoDisponible;
import com.mariposa.biblioteca.dominio.excepciones.UsuarioNoEncontrado;
import com.mariposa.biblioteca.dominio.modelo.Rol;
import com.mariposa.biblioteca.dominio.puertos.entrada.RegistrarPrestamoCasoUso;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.PrestamoRegistrado;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.RegistrarPrestamoComando;
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
import com.mariposa.biblioteca.infraestructura.web.mapeadores.MapeadorWebPrestamo;
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
import java.time.LocalDate;
import java.util.UUID;

import static com.mariposa.biblioteca.infraestructura.web.controladores.AuxiliarAutenticacionPrueba.autenticadoComo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ControladorPrestamo.class)
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
        MapeadorWebPrestamo.class
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
class PruebaControladorPrestamo {

    private static final UUID ID_USUARIO = UUID.fromString("aaaaaaaa-1111-2222-3333-cccccccccccc");
    private static final UUID ID_LIBRO = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID ID_PRESTAMO = UUID.fromString("99999999-aaaa-bbbb-cccc-dddddddddddd");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrarPrestamoCasoUso registrarPrestamoCasoUso;

    @MockitoBean
    private ProveedorToken proveedorToken;

    @Test
    @DisplayName("POST autenticado con cuerpo válido devuelve 201 con Location")
    void registrarExitoso() throws Exception {
        var prestamo = new PrestamoRegistrado(
                ID_PRESTAMO, ID_USUARIO, ID_LIBRO,
                LocalDate.parse("2026-06-06"), LocalDate.parse("2026-06-20"),
                "ACTIVO", Instant.parse("2026-06-06T10:00:00Z")
        );
        given(registrarPrestamoCasoUso.registrar(any(RegistrarPrestamoComando.class))).willReturn(prestamo);

        mockMvc.perform(post("/api/v1/prestamos")
                        .with(autenticadoComo(ID_USUARIO, Rol.USUARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoValido()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.idPrestamo").value(ID_PRESTAMO.toString()))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    @DisplayName("POST sin autenticación devuelve 401")
    void registrarSinAutenticacionDevuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/prestamos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoValido()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST con cuerpo inválido devuelve 400")
    void registrarCuerpoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/prestamos")
                        .with(autenticadoComo(ID_USUARIO, Rol.USUARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idLibro":null,"fechaPrestamo":null,"fechaDevolucionEstimada":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("validacion"));
    }

    @Test
    @DisplayName("POST cuando caso de uso lanza UsuarioNoEncontrado devuelve 404")
    void registrarUsuarioNoEncontradoDevuelve404() throws Exception {
        given(registrarPrestamoCasoUso.registrar(any(RegistrarPrestamoComando.class)))
                .willThrow(new UsuarioNoEncontrado(ID_USUARIO));

        mockMvc.perform(post("/api/v1/prestamos")
                        .with(autenticadoComo(ID_USUARIO, Rol.USUARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoValido()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("usuario-no-encontrado"));
    }

    @Test
    @DisplayName("POST cuando caso de uso lanza ServicioPrestamosNoDisponible devuelve 503")
    void registrarServicioPrestamosNoDisponibleDevuelve503() throws Exception {
        given(registrarPrestamoCasoUso.registrar(any(RegistrarPrestamoComando.class)))
                .willThrow(new ServicioPrestamosNoDisponible("Circuit abierto"));

        mockMvc.perform(post("/api/v1/prestamos")
                        .with(autenticadoComo(ID_USUARIO, Rol.USUARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoValido()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("servicio-prestamos-no-disponible"));
    }

    private String cuerpoValido() {
        var hoy = LocalDate.now();
        var devolucion = hoy.plusDays(14);
        return """
                {
                  "idLibro": "%s",
                  "fechaPrestamo": "%s",
                  "fechaDevolucionEstimada": "%s"
                }
                """.formatted(ID_LIBRO, hoy, devolucion);
    }
}
