package com.mariposa.biblioteca.infraestructura.seguridad;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.URI;
import java.time.Clock;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({
        PropiedadesJwt.class,
        PropiedadesServicioInterno.class,
        PropiedadesLimiteTasaInicioSesion.class
})
public class ConfiguracionSeguridad {

    private static final String PATRON_RUTAS_INTERNAS = "/api/v1/internal/**";
    private static final URI TIPO_PROBLEMA_INTERNO_NO_AUTORIZADO = URI.create("urn:problema:servicio-interno-no-autorizado");

    private static final String[] RUTAS_PUBLICAS_GET = {
            "/actuator/health/**",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private static final String[] RUTAS_PUBLICAS_POST = {
            "/api/v1/autenticacion/**",
            "/api/v1/usuarios"
    };

    @Bean
    public BCryptPasswordEncoder codificadorBCrypt() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public Clock relojSistema() {
        return Clock.systemUTC();
    }

    @Bean
    public FilterRegistrationBean<FiltroAutenticacionJwt> registroFiltroJwt(FiltroAutenticacionJwt filtro) {
        var registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public FilterRegistrationBean<FiltroAutenticacionInterna> registroFiltroInterno(FiltroAutenticacionInterna filtro) {
        var registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public FilterRegistrationBean<FiltroLimiteTasaInicioSesion> registroFiltroLimiteTasa(FiltroLimiteTasaInicioSesion filtro) {
        var registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public CorsConfigurationSource fuenteConfiguracionCors() {
        var configuracion = new CorsConfiguration();
        configuracion.setAllowedOriginPatterns(List.of("*"));
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("*"));
        configuracion.setExposedHeaders(List.of("Authorization", "Location"));
        configuracion.setAllowCredentials(true);
        var fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain cadenaSeguridadInterna(
            HttpSecurity http,
            FiltroAutenticacionInterna filtroAutenticacionInterna,
            EscritorRespuestaProblema escritorRespuestaProblema
    ) throws Exception {
        return http
                .securityMatcher(PATRON_RUTAS_INTERNAS)
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(autorizacion -> autorizacion
                        .anyRequest().hasRole("SERVICIO_INTERNO"))
                .exceptionHandling(excepciones -> excepciones
                        .authenticationEntryPoint((request, response, authException) ->
                                escribirRespuestaNoAutenticado(response, escritorRespuestaProblema, request.getRequestURI()))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                escribirRespuestaNoAutenticado(response, escritorRespuestaProblema, request.getRequestURI())))
                .addFilterBefore(filtroAutenticacionInterna, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain cadenaFiltrosSeguridad(
            HttpSecurity http,
            FiltroAutenticacionJwt filtroAutenticacionJwt,
            FiltroLimiteTasaInicioSesion filtroLimiteTasaInicioSesion,
            PuntoEntradaAutenticacionJwt puntoEntradaAutenticacionJwt,
            ManejadorAccesoDenegadoJwt manejadorAccesoDenegadoJwt,
            CorsConfigurationSource fuenteConfiguracionCors
    ) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(fuenteConfiguracionCors))
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(autorizacion -> autorizacion
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, RUTAS_PUBLICAS_POST).permitAll()
                        .requestMatchers(HttpMethod.GET, RUTAS_PUBLICAS_GET).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(excepciones -> excepciones
                        .authenticationEntryPoint(puntoEntradaAutenticacionJwt)
                        .accessDeniedHandler(manejadorAccesoDenegadoJwt))
                .addFilterBefore(filtroLimiteTasaInicioSesion, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(filtroAutenticacionJwt, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private void escribirRespuestaNoAutenticado(
            HttpServletResponse response,
            EscritorRespuestaProblema escritor,
            String ruta
    ) throws java.io.IOException {
        var problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Servicio interno no autorizado");
        problema.setTitle("Acceso interno denegado");
        problema.setType(TIPO_PROBLEMA_INTERNO_NO_AUTORIZADO);
        problema.setInstance(URI.create(ruta));
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        escritor.escribir(response, problema);
    }
}
