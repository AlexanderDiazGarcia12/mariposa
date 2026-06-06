# Mariposa — Sistema de Biblioteca Distribuido

Sistema distribuido de gestión de biblioteca compuesto por dos microservicios: catálogo y autenticación en Java 21 / Spring Boot 4, y gestión de préstamos en Go. Comunicación REST resiliente, persistencia en PostgreSQL 16 y despliegue con Docker Compose.

Prueba técnica para Grupo Mariposa.

## Tabla de Contenidos

- [Visión General](#visión-general)
- [Arquitectura](#arquitectura)
- [Stack Tecnológico](#stack-tecnológico)
- [Requisitos](#requisitos)
- [Configuración Local](#configuración-local)
- [Ejecución con Docker](#ejecución-con-docker)
- [Endpoints Expuestos](#endpoints-expuestos)
- [Documentación de la API](#documentación-de-la-api)
- [Pruebas](#pruebas)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Decisiones Técnicas](#decisiones-técnicas)
- [Licencia](#licencia)

## Visión General

> Pendiente de completar.

## Arquitectura

> Pendiente de completar (diagrama + descripción de microservicios y comunicación).

## Stack Tecnológico

| Componente | Tecnología |
|------------|-----------|
| Servicio Biblioteca | Java 21, Spring Boot 4, Spring Security, JPA/Hibernate |
| Servicio Préstamos | Go 1.22+ |
| Base de datos | PostgreSQL 16 |
| Migraciones | Flyway |
| Comunicación inter-servicios | REST (HTTP) + gRPC (bonus) |
| Seguridad | JWT (HS256) + BCrypt |
| Contenedores | Docker, Docker Compose |
| Pruebas | JUnit 5, Mockito, Testcontainers, WireMock |
| Documentación API | OpenAPI 3 / Swagger UI |
| CI | GitHub Actions |

## Requisitos

- JDK 21
- Maven 3.9+
- Go 1.22+
- Docker y Docker Compose v2
- PostgreSQL 16 (o usar el contenedor incluido)

## Configuración Local

1. Clona el repositorio.
2. Copia la plantilla de variables:

   ```bash
   cp .env.example .env
   ```

3. Edita `.env` con tus valores locales (mínimo `POSTGRES_PASSWORD` y `JWT_CLAVE_SECRETA`).

## Ejecución con Docker

```bash
docker compose up -d
```

Esto levanta la infraestructura (PostgreSQL 16) y los microservicios disponibles. Para detener:

```bash
docker compose down
```

Para reiniciar con datos limpios:

```bash
docker compose down -v
```

## Endpoints Expuestos

> Pendiente de completar.

## Documentación de la API

Una vez levantado el servicio: http://localhost:8080/swagger-ui.html

## Pruebas

```bash
cd servicio-biblioteca
./mvnw test
```

## Estructura del Proyecto

```
mariposa/
├── servicio-biblioteca/   Microservicio Spring Boot (Java 21)
├── servicio-prestamos/    Microservicio Go (pendiente)
├── docs/                  Documentación adicional y diagramas
├── .github/workflows/     Pipelines de CI
├── docker-compose.yml     Orquestación local
├── .env.example           Plantilla de variables de entorno
└── README.md
```

## Decisiones Técnicas

> Pendiente de completar (justificación de patrones, librerías, trade-offs).

## Licencia

MIT — ver archivo [LICENSE](./LICENSE).
