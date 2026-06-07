package com.mariposa.biblioteca.infraestructura.web.controladores;

import com.mariposa.biblioteca.dominio.excepciones.CredencialesInvalidas;
import com.mariposa.biblioteca.dominio.modelo.EstadoUsuario;
import com.mariposa.biblioteca.dominio.modelo.Rol;
import com.mariposa.biblioteca.dominio.modelo.Usuario;
import com.mariposa.biblioteca.dominio.modelo.valor.CorreoElectronico;
import com.mariposa.biblioteca.dominio.puertos.entrada.AutenticarUsuarioCasoUso;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.CredencialesComando;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.ResultadoAutenticacion;
import com.mariposa.biblioteca.dominio.puertos.salida.ProveedorToken;
import com.mariposa.biblioteca.infraestructura.seguridad.ConfiguracionSeguridad;
import com.mariposa.biblioteca.infraestructura.seguridad.EscritorRespuestaProblema;
import com.mariposa.biblioteca.infraestructura.seguridad.FiltroAutenticacionInterna;
import com.mariposa.biblioteca.infraestructura.seguridad.FiltroAutenticacionJwt;
import com.mariposa.biblioteca.infraestructura.seguridad.ManejadorAccesoDenegadoJwt;
import com.mariposa.biblioteca.infraestructura.seguridad.PuntoEntradaAutenticacionJwt;
import com.mariposa.biblioteca.infraestructura.web.manejadores.ManejadorGlobalExcepciones;
import com.mariposa.biblioteca.infraestructura.web.mapeadores.MapeadorWebAutenticacion;
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
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ControladorAutenticacion.class)
@Import({
        ConfiguracionSeguridad.class,
        FiltroAutenticacionJwt.class,
        FiltroAutenticacionInterna.class,
        PuntoEntradaAutenticacionJwt.class,
        ManejadorAccesoDenegadoJwt.class,
        EscritorRespuestaProblema.class,
        ManejadorGlobalExcepciones.class,
        MapeadorWebAutenticacion.class,
        MapeadorWebUsuario.class
})
@TestPropertySource(properties = {
        "seguridad.jwt.clave-secreta=clave-de-prueba-de-minimo-32-bytes-para-HS256-tests-mariposa",
        "seguridad.jwt.duracion-acceso-minutos=15",
        "seguridad.jwt.duracion-refresco-dias=7",
        "seguridad.jwt.emisor=mariposa-biblioteca",
        "seguridad.interno.secreto=secreto-de-prueba-minimo-16-caracteres"
})
class PruebaControladorAutenticacion {

    private static final UUID ID_USUARIO = UUID.fromString("aaaaaaaa-1111-2222-3333-cccccccccccc");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AutenticarUsuarioCasoUso autenticarUsuarioCasoUso;

    @MockitoBean
    private ProveedorToken proveedorToken;

    @Test
    @DisplayName("POST iniciar-sesion con credenciales válidas devuelve 200 y tokens")
    void iniciarSesionDevuelveTokens() throws Exception {
        var resultado = new ResultadoAutenticacion(
                "token-acceso",
                "token-refresco",
                Instant.parse("2026-06-05T11:00:00Z"),
                construirUsuario()
        );
        given(autenticarUsuarioCasoUso.iniciarSesion(any(CredencialesComando.class))).willReturn(resultado);

        mockMvc.perform(post("/api/v1/autenticacion/iniciar-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreUsuario":"diana.lopez","contrasena":"Contrasena.Segura.123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenAcceso").value("token-acceso"))
                .andExpect(jsonPath("$.tokenRefresco").value("token-refresco"))
                .andExpect(jsonPath("$.usuario.nombreUsuario").value("diana.lopez"))
                .andExpect(jsonPath("$.expiraEn").value(notNullValue()));
    }

    @Test
    @DisplayName("POST iniciar-sesion con credenciales inválidas devuelve 401")
    void iniciarSesionCredencialesInvalidasDevuelve401() throws Exception {
        given(autenticarUsuarioCasoUso.iniciarSesion(any(CredencialesComando.class)))
                .willThrow(new CredencialesInvalidas());

        mockMvc.perform(post("/api/v1/autenticacion/iniciar-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreUsuario":"x","contrasena":"y"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("credenciales-invalidas"));
    }

    @Test
    @DisplayName("POST refrescar con token válido devuelve 200")
    void refrescarExitoso() throws Exception {
        var resultado = new ResultadoAutenticacion(
                "nuevo-acceso", "nuevo-refresco",
                Instant.parse("2026-06-05T12:00:00Z"), construirUsuario()
        );
        given(autenticarUsuarioCasoUso.refrescarToken("token-refresco")).willReturn(resultado);

        mockMvc.perform(post("/api/v1/autenticacion/refrescar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tokenRefresco":"token-refresco"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenAcceso").value("nuevo-acceso"));
    }

    @Test
    @DisplayName("POST iniciar-sesion sin nombreUsuario devuelve 400")
    void iniciarSesionSinNombreDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/autenticacion/iniciar-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contrasena":"abc"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("validacion"))
                .andExpect(jsonPath("$.errores.nombreUsuario").value(notNullValue()));
    }

    private Usuario construirUsuario() {
        var ahora = Instant.parse("2026-01-01T00:00:00Z");
        return new Usuario(
                ID_USUARIO, "diana.lopez",
                CorreoElectronico.desTexto("diana@correo.com"),
                "$2a$10$hashbcryptficticio0123456789ABCDEFGHIJKLMNOPQRSTUV",
                Rol.USUARIO, EstadoUsuario.ACTIVO, ahora, ahora
        );
    }
}
