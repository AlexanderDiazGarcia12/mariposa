package com.mariposa.biblioteca.infraestructura.web.configuracion;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ConfiguracionOpenApi {

    private static final String NOMBRE_ESQUEMA_SEGURIDAD = "bearerAuth";

    @Bean
    public OpenAPI personalizarOpenApi() {
        return new OpenAPI()
                .info(construirInfo())
                .servers(construirServidores())
                .components(construirComponentes());
    }

    private Info construirInfo() {
        return new Info()
                .title("Servicio Biblioteca — API REST")
                .description(
                        "API REST de gestión de catálogo y autenticación de la biblioteca distribuida. "
                                + "Forma parte del sistema Mariposa junto al microservicio Préstamos (Go). "
                                + "Documentación generada automáticamente desde anotaciones springdoc-openapi."
                )
                .version("0.0.1-SNAPSHOT")
                .contact(construirContacto())
                .license(construirLicencia());
    }

    private Contact construirContacto() {
        return new Contact()
                .name("Alexander Díaz")
                .email("alexander.diazg@outlook.com")
                .url("https://github.com/AlexanderDiazGarcia12/mariposa");
    }

    private License construirLicencia() {
        return new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");
    }

    private List<Server> construirServidores() {
        return List.of(
                new Server()
                        .url("http://localhost:8080")
                        .description("Entorno local (docker compose)")
        );
    }

    private Components construirComponentes() {
        return new Components()
                .addSecuritySchemes(NOMBRE_ESQUEMA_SEGURIDAD, construirEsquemaBearer());
    }

    private SecurityScheme construirEsquemaBearer() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description(
                        "Token JWT obtenido vía POST /api/v1/autenticacion/iniciar-sesion. "
                                + "Incluir en header: Authorization: Bearer <token>"
                );
    }
}
