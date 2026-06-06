package com.mariposa.biblioteca.infraestructura.seguridad;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

import java.time.Clock;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(PropiedadesJwt.class)
public class ConfiguracionSeguridad {

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
    public SecurityFilterChain cadenaFiltrosSeguridad(
            HttpSecurity http,
            FiltroAutenticacionJwt filtroAutenticacionJwt,
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
                .addFilterBefore(filtroAutenticacionJwt, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
