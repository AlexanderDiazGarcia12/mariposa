package com.mariposa.biblioteca.infraestructura.web.manejadores;

import com.mariposa.biblioteca.dominio.excepciones.CredencialesInvalidas;
import com.mariposa.biblioteca.dominio.excepciones.ExcepcionDominio;
import com.mariposa.biblioteca.dominio.excepciones.LibroNoEncontrado;
import com.mariposa.biblioteca.dominio.excepciones.LibroNoEncontradoPorIsbn;
import com.mariposa.biblioteca.dominio.excepciones.LibroYaExiste;
import com.mariposa.biblioteca.dominio.excepciones.OperacionInvalidaDominio;
import com.mariposa.biblioteca.dominio.excepciones.PrestamoRechazadoPorServicioB;
import com.mariposa.biblioteca.dominio.excepciones.ServicioPrestamosNoDisponible;
import com.mariposa.biblioteca.dominio.excepciones.SinCopiasDisponibles;
import com.mariposa.biblioteca.dominio.excepciones.UsuarioNoEncontrado;
import com.mariposa.biblioteca.dominio.excepciones.UsuarioNoEncontradoPorNombre;
import com.mariposa.biblioteca.dominio.excepciones.UsuarioYaExiste;
import com.mariposa.biblioteca.dominio.excepciones.ValorInvalidoDominio;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

    private static final String TIPO_BASE = "urn:problema:";

    @ExceptionHandler(ExcepcionDominio.class)
    public ResponseEntity<ProblemDetail> manejarDominio(
            ExcepcionDominio excepcion,
            HttpServletRequest peticion
    ) {
        var problema = mapearDominio(excepcion);
        problema.setInstance(URI.create(peticion.getRequestURI()));
        return ResponseEntity.status(problema.getStatus()).body(problema);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> manejarValidacionCuerpo(
            MethodArgumentNotValidException excepcion,
            HttpServletRequest peticion
    ) {
        var errores = new LinkedHashMap<String, String>();
        excepcion.getBindingResult().getFieldErrors().forEach(error ->
                errores.put(error.getField(), error.getDefaultMessage())
        );
        var problema = construirProblema(
                HttpStatus.BAD_REQUEST,
                "Solicitud inválida",
                "Existen errores de validación en los campos enviados",
                "validacion"
        );
        problema.setProperty("errores", errores);
        problema.setInstance(URI.create(peticion.getRequestURI()));
        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> manejarValidacionParametros(
            HandlerMethodValidationException excepcion,
            HttpServletRequest peticion
    ) {
        var problema = construirProblema(
                HttpStatus.BAD_REQUEST,
                "Solicitud inválida",
                "Existen errores de validación en los parámetros",
                "validacion-parametros"
        );
        problema.setInstance(URI.create(peticion.getRequestURI()));
        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> manejarCuerpoIlegible(
            HttpMessageNotReadableException excepcion,
            HttpServletRequest peticion
    ) {
        var problema = construirProblema(
                HttpStatus.BAD_REQUEST,
                "Cuerpo de la solicitud inválido",
                "No se pudo leer el cuerpo de la solicitud",
                "cuerpo-invalido"
        );
        problema.setInstance(URI.create(peticion.getRequestURI()));
        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> manejarTipoArgumentoInvalido(
            MethodArgumentTypeMismatchException excepcion,
            HttpServletRequest peticion
    ) {
        var problema = construirProblema(
                HttpStatus.BAD_REQUEST,
                "Parámetro inválido",
                "El parámetro '%s' tiene un valor inválido".formatted(excepcion.getName()),
                "parametro-invalido"
        );
        problema.setProperty("parametro", excepcion.getName());
        problema.setInstance(URI.create(peticion.getRequestURI()));
        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ProblemDetail> manejarAutorizacionDenegada(
            AuthorizationDeniedException excepcion,
            HttpServletRequest peticion
    ) {
        var problema = construirProblema(
                HttpStatus.FORBIDDEN,
                "Acceso denegado",
                "No tiene autorización para acceder a este recurso",
                "acceso-denegado"
        );
        problema.setInstance(URI.create(peticion.getRequestURI()));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problema);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> manejarErrorGeneral(
            Exception excepcion,
            HttpServletRequest peticion
    ) {
        LOGGER.error("Error inesperado procesando la petición {}", peticion.getRequestURI(), excepcion);
        var problema = construirProblema(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno",
                "Ocurrió un error inesperado procesando la solicitud",
                "error-interno"
        );
        problema.setInstance(URI.create(peticion.getRequestURI()));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problema);
    }

    private ProblemDetail mapearDominio(ExcepcionDominio excepcion) {
        return switch (excepcion) {
            case LibroNoEncontrado e -> problemaDominio(
                    HttpStatus.NOT_FOUND, "Libro no encontrado", e.getMessage(),
                    "libro-no-encontrado", Map.of("idLibro", e.idLibro())
            );
            case LibroNoEncontradoPorIsbn e -> problemaDominio(
                    HttpStatus.NOT_FOUND, "Libro no encontrado", e.getMessage(),
                    "libro-no-encontrado-isbn", Map.of("isbn", e.isbn().valor())
            );
            case UsuarioNoEncontrado e -> problemaDominio(
                    HttpStatus.NOT_FOUND, "Usuario no encontrado", e.getMessage(),
                    "usuario-no-encontrado", Map.of("idUsuario", e.idUsuario())
            );
            case UsuarioNoEncontradoPorNombre e -> problemaDominio(
                    HttpStatus.NOT_FOUND, "Usuario no encontrado", e.getMessage(),
                    "usuario-no-encontrado-nombre", Map.of("nombreUsuario", e.nombreUsuario())
            );
            case LibroYaExiste e -> problemaDominio(
                    HttpStatus.CONFLICT, "Libro ya existe", e.getMessage(),
                    "libro-ya-existe", Map.of("isbn", e.isbn().valor())
            );
            case UsuarioYaExiste e -> problemaDominio(
                    HttpStatus.CONFLICT, "Usuario ya existe", e.getMessage(),
                    "usuario-ya-existe", Map.of("identificador", e.identificador())
            );
            case SinCopiasDisponibles e -> problemaDominio(
                    HttpStatus.CONFLICT, "Sin copias disponibles", e.getMessage(),
                    "sin-copias-disponibles", Map.of("idLibro", e.idLibro())
            );
            case OperacionInvalidaDominio e -> problemaDominio(
                    HttpStatus.CONFLICT, "Operación inválida", e.getMessage(),
                    "operacion-invalida", Map.of()
            );
            case ValorInvalidoDominio e -> problemaDominio(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Valor inválido", e.getMessage(),
                    "valor-invalido", Map.of("campo", e.campo())
            );
            case CredencialesInvalidas e -> problemaDominio(
                    HttpStatus.UNAUTHORIZED, "Credenciales inválidas", e.getMessage(),
                    "credenciales-invalidas", Map.of()
            );
            case ServicioPrestamosNoDisponible e -> problemaDominio(
                    HttpStatus.SERVICE_UNAVAILABLE, "Servicio de préstamos no disponible", e.getMessage(),
                    "servicio-prestamos-no-disponible", Map.of()
            );
            case PrestamoRechazadoPorServicioB e -> problemaDominio(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Préstamo rechazado", e.getMessage(),
                    "prestamo-rechazado", Map.of("codigoError", e.codigoError())
            );
        };
    }

    private ProblemDetail problemaDominio(
            HttpStatusCode estado,
            String titulo,
            String detalle,
            String codigo,
            Map<String, Object> atributos
    ) {
        var problema = construirProblema(estado, titulo, detalle, codigo);
        atributos.forEach(problema::setProperty);
        return problema;
    }

    private ProblemDetail construirProblema(
            HttpStatusCode estado,
            String titulo,
            String detalle,
            String codigo
    ) {
        var problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        problema.setType(URI.create(TIPO_BASE + codigo));
        problema.setProperty("codigo", codigo);
        return problema;
    }
}
