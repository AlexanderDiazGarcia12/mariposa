# Mariposa — Sistema de Biblioteca Distribuido

[![CI](https://github.com/AlexanderDiazGarcia12/mariposa/actions/workflows/ci.yml/badge.svg)](https://github.com/AlexanderDiazGarcia12/mariposa/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen)](https://spring.io/projects/spring-boot)
[![Go](https://img.shields.io/badge/Go-1.25-00ADD8)](https://go.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791)](https://www.postgresql.org/)

Sistema distribuido de gestión de biblioteca para la prueba técnica de **Grupo Mariposa**. Está compuesto por dos microservicios que se comunican vía REST sobre HTTP, cada uno con su propia base de datos PostgreSQL:

- **Servicio Biblioteca (A)** — Java 21 + Spring Boot 4. Gestiona el catálogo de libros, los usuarios y la autenticación basada en JWT. Orquesta el registro de préstamos llamando al Servicio B.
- **Servicio Préstamos (B)** — Go 1.25. Gestiona el ciclo de vida de los préstamos (registro, consulta, devolución). Valida disponibilidad de libros consultando un endpoint interno del Servicio A.

Ambos servicios siguen **arquitectura hexagonal** (puertos / adaptadores), tienen su propia base de datos PostgreSQL 16 con migraciones versionadas, y son contenedorizables con Docker.

> **Convenciones del proyecto:** todo el dominio, paquetes, clases, métodos, variables, columnas SQL y mensajes están escritos **100% en español**.

## Tabla de Contenidos

- [Visión General](#visión-general)
- [Arquitectura](#arquitectura)
- [Stack Tecnológico](#stack-tecnológico)
- [Requisitos](#requisitos)
- [Configuración Local](#configuración-local)
- [Ejecución con Docker](#ejecución-con-docker)
- [Endpoints Expuestos](#endpoints-expuestos)
- [Flujo Completo de Préstamo (cURL)](#flujo-completo-de-préstamo-curl)
- [Documentación de la API](#documentación-de-la-api)
- [Pruebas](#pruebas)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Decisiones Técnicas](#decisiones-técnicas)
- [Licencia](#licencia)

## Visión General

El sistema modela tres conceptos de negocio:

| Entidad | Servicio dueño | Responsabilidad |
|---------|----------------|-----------------|
| `Usuario` | Servicio A | Autenticación, perfil y roles (`ADMINISTRADOR`, `USUARIO`). |
| `Libro` | Servicio A | Catálogo, copias totales y disponibles, búsqueda y filtrado. |
| `Préstamo` | Servicio B | Registro de quién prestó qué libro y cuándo, devolución y consulta histórica. |

Cada microservicio es dueño exclusivo de su modelo y de su base de datos: el Servicio B nunca consulta directamente la tabla de libros; pide la información a través de un endpoint REST interno protegido por header secreto.

## Arquitectura

### Topología

```
            Cliente (frontend, cURL, Postman)
                          │
                          │  JWT (HS256, header Authorization)
                          ▼
        ┌──────────────────────────────────────┐
        │       Servicio Biblioteca (A)         │
        │       Java 21 + Spring Boot 4         │
        │                                       │
        │  Controladores REST + Spring Security │
        │  Servicios de aplicación (casos uso)  │
        │  Dominio rico (records inmutables)    │
        │  Repositorios JPA / Specifications    │
        └────────┬───────────────────────┬──────┘
                 │                       │
   X-Servicio-Interno  ◄──┐              │  RestClient + Resilience4j
   (HTTP, header secreto) │              │  (CircuitBreaker + Retry)
                 │        │              ▼
                 │        │   ┌──────────────────────────────┐
                 │        │   │     Servicio Préstamos (B)    │
                 │        │   │     Go 1.25 + net/http        │
                 │        │   │                               │
                 │        │   │  Controladores HTTP           │
                 │        │   │  Casos de uso                 │
                 │        │   │  Dominio (aggregates)         │
                 │        │   │  Repositorio pgx + migraciones│
                 │        └───┤  Cliente HTTP al Servicio A   │
                 │            └───────────────┬───────────────┘
                 │                            │
                 ▼                            ▼
        ┌──────────────────┐         ┌──────────────────┐
        │ PostgreSQL 16    │         │ PostgreSQL 16    │
        │ biblioteca       │         │ prestamos        │
        │ (usuarios+libros)│         │ (prestamos)      │
        └──────────────────┘         └──────────────────┘
```

### Arquitectura hexagonal en cada servicio

Ambos servicios separan el código en tres anillos:

```
   ┌─────────────────────────────────────────────────┐
   │              infraestructura                    │
   │   (web, persistencia, seguridad, clientes)      │
   │   ┌─────────────────────────────────────────┐   │
   │   │            aplicación                   │   │
   │   │   (casos de uso, servicios de app)      │   │
   │   │   ┌─────────────────────────────────┐   │   │
   │   │   │           dominio               │   │   │
   │   │   │   (modelo, puertos, errores)    │   │   │
   │   │   └─────────────────────────────────┘   │   │
   │   └─────────────────────────────────────────┘   │
   └─────────────────────────────────────────────────┘
```

- **Dominio** no depende de nada externo (ni Spring, ni JPA, ni HTTP). En Java son `record` y `sealed`; en Go son structs con campos privados y constructores con validación.
- **Aplicación** orquesta el caso de uso usando solamente puertos del dominio.
- **Infraestructura** implementa los puertos y conecta el sistema con el mundo (REST, BD, JWT, otro microservicio).

### Flujo de registro de préstamo (A↔B)

```
1. Cliente ──Bearer JWT──▶ POST /api/v1/prestamos (Servicio A)
2. Servicio A valida usuario activo + libro existente + copias > 0
3. Servicio A invoca dominio: libro.prestar() → nuevo agregado decrementado
4. Servicio A ──RestClient──▶ POST /api/v1/prestamos (Servicio B)
   (envuelto en @CircuitBreaker + @Retry de Resilience4j)
5. Servicio B persiste el préstamo en su BD
6. Servicio B ──HTTP──▶ GET /api/v1/internal/libros/{id} (Servicio A)
   (con header X-Servicio-Interno: <secreto>) — para validar disponibilidad
7. Servicio B responde 201 con el préstamo creado
8. Servicio A guarda el libro decrementado (mismo transactional context)
9. Servicio A devuelve 201 al cliente con el préstamo registrado
```

> Si el paso 4–7 falla, la transacción local en A no comitea: no hay decremento espurio.

## Stack Tecnológico

| Componente | Tecnología |
|------------|-----------|
| Servicio Biblioteca | Java 21, Spring Boot 4.0.6, Spring Security 7, Spring Data JPA / Hibernate |
| Servicio Préstamos | Go 1.25, `net/http` (stdlib), `log/slog`, `pgx v5` |
| Base de datos | PostgreSQL 16 (una instancia por servicio) |
| Migraciones | Flyway (A), `golang-migrate` + `embed.FS` (B) |
| Comunicación inter-servicios | REST sobre HTTP con header secreto `X-Servicio-Interno` |
| Seguridad | JWT HS256 (JJWT 0.12) + BCrypt + dos `SecurityFilterChain` por orden |
| Resiliencia | Resilience4j 2.4 (CircuitBreaker + Retry) en el cliente HTTP del Servicio A |
| Rate limiting | Bucket4j 8.19 (token bucket en memoria) en `POST /api/v1/autenticacion/iniciar-sesion` |
| Observabilidad | Logs JSON estructurados en ambos servicios + trace ID `X-Request-Id` propagado A↔B para correlación distribuida |
| Contenedores | Docker multi-stage + Docker Compose v2 |
| Pruebas | JUnit 5, Mockito, Testcontainers, WireMock (A); `testing`, `testify`, `testcontainers-go` (B) |
| Documentación API | OpenAPI 3 / Swagger UI vía Springdoc 3.0.3 (Servicio A) |

## Requisitos

| Herramienta | Versión mínima |
|-------------|---------------|
| JDK | 21 (Eclipse Temurin recomendado) |
| Maven Wrapper | incluido (`./mvnw`) |
| Go | 1.25+ |
| Docker | 24+ |
| Docker Compose | v2 |

> Para ejecutar todo con Docker no necesitas JDK ni Go instalados localmente.

## Configuración Local

```bash
git clone git@github.com:AlexanderDiazGarcia12/mariposa.git
cd mariposa
cp .env.example .env
```

Edita `.env` y como mínimo cambia:

- `POSTGRES_PASSWORD` y `POSTGRES_PRESTAMOS_PASSWORD`
- `JWT_CLAVE_SECRETA` (mínimo 32 bytes para HS256)
- `SERVICIO_INTERNO_SECRETO` (debe coincidir entre A y B)

## Ejecución con Docker

```bash
docker compose up -d --build
```

Esto levanta cuatro contenedores:

| Contenedor | Imagen | Puerto host |
|------------|--------|-------------|
| `mariposa-postgres` | `postgres:16-alpine` | `5432` |
| `mariposa-postgres-prestamos` | `postgres:16-alpine` | `5433` |
| `mariposa-servicio-biblioteca` | Construida desde `./servicio-biblioteca/Dockerfile` | `8080` |
| `mariposa-servicio-prestamos` | Construida desde `./servicio-prestamos/Dockerfile` | `8081` |

Verificar estado y healthchecks:

```bash
docker compose ps
```

Apagar:

```bash
docker compose down          # mantiene los volúmenes
docker compose down -v       # los borra
```

## Endpoints Expuestos

### Servicio Biblioteca (A) — `http://localhost:8080`

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| POST | `/api/v1/autenticacion/iniciar-sesion` | Pública | Iniciar sesión, devuelve `tokenAcceso` + `tokenRefresco`. |
| POST | `/api/v1/autenticacion/refrescar` | Pública (con token de refresco) | Renueva el token de acceso. |
| POST | `/api/v1/autenticacion/cerrar-sesion` | Bearer JWT | Cierra la sesión y revoca el refresco. |
| GET | `/api/v1/usuarios` | `ROLE_ADMINISTRADOR` | Listar / filtrar usuarios. |
| POST | `/api/v1/usuarios` | `ROLE_ADMINISTRADOR` | Crear un usuario. |
| GET | `/api/v1/usuarios/{id}` | `ROLE_ADMINISTRADOR` o el propio usuario | Consultar un usuario. |
| PATCH | `/api/v1/usuarios/{id}` | Reglas por campo | Actualizar perfil o rol. |
| DELETE | `/api/v1/usuarios/{id}` | `ROLE_ADMINISTRADOR` | Desactivar usuario. |
| GET | `/api/v1/libros` | Bearer JWT | Listar / filtrar / paginar el catálogo. |
| POST | `/api/v1/libros` | `ROLE_ADMINISTRADOR` | Crear libro. |
| GET | `/api/v1/libros/{id}` | Bearer JWT | Consultar libro. |
| PATCH | `/api/v1/libros/{id}` | `ROLE_ADMINISTRADOR` | Actualizar catálogo. |
| DELETE | `/api/v1/libros/{id}` | `ROLE_ADMINISTRADOR` | Eliminar libro. |
| POST | `/api/v1/prestamos` | Bearer JWT | Registrar préstamo (orquesta A→B). |
| GET | `/api/v1/internal/libros/{id}` | Header `X-Servicio-Interno` | Endpoint M2M usado por el Servicio B. |
| GET | `/actuator/health` | Pública | Healthcheck. |
| GET | `/swagger-ui.html` | Pública | UI interactiva de OpenAPI. |
| GET | `/v3/api-docs` | Pública | Spec OpenAPI 3 en JSON. |

### Servicio Préstamos (B) — `http://localhost:8081`

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| POST | `/api/v1/prestamos` | Pública (interna a la red) | Registrar préstamo. |
| GET | `/api/v1/prestamos` | Pública | Listar préstamos (paginado). |
| GET | `/api/v1/prestamos/{id}` | Pública | Consultar préstamo. |
| POST | `/api/v1/prestamos/{id}/devolver` | Pública | Marcar como devuelto. |
| GET | `/api/v1/prestamos/usuario/{idUsuario}` | Pública | Listar préstamos por usuario. |
| GET | `/salud` | Pública | Healthcheck. |

> El Servicio B es interno: en producción quedaría detrás de un API Gateway o sólo accesible desde la red privada. Todos los errores se devuelven con `application/problem+json` (RFC 7807).

## Flujo Completo de Préstamo (cURL)

### 1. Iniciar sesión como administrador

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/autenticacion/iniciar-sesion \
  -H 'Content-Type: application/json' \
  -d '{"nombreUsuario":"admin","contrasena":"Admin123!"}' \
  | jq -r '.tokenAcceso')
echo "$TOKEN"
```

> El usuario `admin` se crea automáticamente con Flyway `V2__usuario_administrador_inicial.sql`.

### 2. Crear un libro con 3 copias

```bash
ID_LIBRO=$(curl -s -X POST http://localhost:8080/api/v1/libros \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
        "titulo":"El Quijote",
        "autor":"Miguel de Cervantes",
        "isbn":"9788491050292",
        "anioPublicacion":1605,
        "copiasTotales":3,
        "copiasDisponibles":3
      }' | jq -r '.id')
echo "$ID_LIBRO"
```

### 3. Registrar un préstamo (orquesta A→B→A)

```bash
HOY=$(date -u +%Y-%m-%d)
curl -s -X POST http://localhost:8080/api/v1/prestamos \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{
        \"idLibro\":\"$ID_LIBRO\",
        \"fechaPrestamo\":\"$HOY\",
        \"fechaDevolucionEstimada\":\"$(date -u -v+14d +%Y-%m-%d)\"
      }" | jq
```

### 4. Verificar que las copias bajaron en el Servicio A

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/libros/$ID_LIBRO | jq '.copiasDisponibles'
# → 2
```

### 5. Verificar el préstamo registrado en el Servicio B

```bash
curl -s http://localhost:8081/api/v1/prestamos | jq
```

### 6. Probar el endpoint interno (M2M)

```bash
# Sin header → 401
curl -i http://localhost:8080/api/v1/internal/libros/$ID_LIBRO

# Con header → 200
curl -i -H "X-Servicio-Interno: $SERVICIO_INTERNO_SECRETO" \
  http://localhost:8080/api/v1/internal/libros/$ID_LIBRO
```

Más ejemplos en [`docs/pruebas-curl.md`](./docs/pruebas-curl.md).

## Documentación de la API

Servicio A expone OpenAPI 3 con Springdoc:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Spec JSON:** http://localhost:8080/v3/api-docs

Los endpoints están agrupados en tags: `Autenticación`, `Usuarios`, `Libros`, `Préstamos`, `Servicios Internos`, `Actuator`.

## Pruebas

### Servicio Biblioteca (A)

```bash
cd servicio-biblioteca
./mvnw test
```

- 226 tests (unitarios + integración con Testcontainers + cliente HTTP con WireMock).
- Requiere Docker en marcha para los tests de Testcontainers.

### Servicio Préstamos (B)

```bash
cd servicio-prestamos
go test ./...
```

- 42 tests (dominio + casos de uso + cliente HTTP con `httptest` + repositorio con `testcontainers-go` + handlers HTTP).
- Para omitir los tests de integración: `OMITIR_TESTS_INTEGRACION=1 go test ./...`.

### Smoke test end-to-end

```bash
docker compose up -d --build
# Esperar a que ambos servicios estén "healthy":
docker compose ps
# Luego ejecutar el flujo cURL de la sección anterior.
```

## Estructura del Proyecto

```
mariposa/
├── servicio-biblioteca/                    Microservicio A (Java 21 + Spring Boot 4)
│   ├── src/main/java/com/mariposa/biblioteca/
│   │   ├── dominio/                        Modelo, puertos, excepciones (sin Spring)
│   │   ├── aplicacion/servicios/           Casos de uso
│   │   └── infraestructura/
│   │       ├── web/                        Controladores, DTOs, mapeadores, manejadores
│   │       ├── persistencia/               Entidades JPA, repositorios, specifications
│   │       ├── seguridad/                  JWT, BCrypt, filtros, dos SecurityFilterChain
│   │       └── clientes/prestamos/         RestClient + Resilience4j hacia el Servicio B
│   ├── src/main/resources/
│   │   ├── db/migration/                   Flyway V1, V2
│   │   ├── application.yml                 Configuración común
│   │   ├── application-local.yml           Perfil de desarrollo
│   │   └── application-docker.yml          Perfil para Docker Compose
│   ├── src/test/java/                      Tests unitarios + integración + WireMock
│   ├── pom.xml
│   └── Dockerfile                          Multi-stage temurin:21-jdk-alpine → 21-jre-alpine
│
├── servicio-prestamos/                     Microservicio B (Go 1.25)
│   ├── cmd/servidor/main.go                Wiring + graceful shutdown
│   ├── internal/
│   │   ├── dominio/                        Prestamo, EstadoPrestamo, errores sentinela
│   │   ├── aplicacion/                     Casos de uso + puertos
│   │   ├── infraestructura/
│   │   │   ├── persistencia/               Repositorio pgx + migraciones embebidas
│   │   │   ├── cliente_biblioteca/         Cliente HTTP hacia el Servicio A
│   │   │   └── web/                        Servidor, middleware, controladores, DTOs
│   │   └── configuracion/                  Carga de variables de entorno
│   ├── go.mod / go.sum
│   └── Dockerfile                          Multi-stage golang:1.25-alpine → alpine:3.20 (~35MB)
│
├── docs/
│   └── pruebas-curl.md                     Ejemplos cURL del flujo completo
├── .github/workflows/ci.yml                Pipeline CI (tests Java + Go + build Docker)
├── .env.example                            Plantilla de variables de entorno
├── docker-compose.yml                      Orquestación local de los 4 contenedores
├── LICENSE                                 MIT
└── README.md
```

## Decisiones Técnicas

### Arquitectura hexagonal en ambos servicios

El dominio queda aislado de frameworks: en Java es `record` + `sealed` sin anotaciones de Spring ni JPA; en Go es struct con campos privados y validación en el constructor. Esto permite testear reglas de negocio sin Spring/contenedores y cambiar adaptadores (REST por gRPC, JPA por pgx) sin tocar el corazón.

### Records inmutables como modelo de dominio (Java 21)

`Libro` y `Usuario` son `record`. Operaciones como `libro.prestar()` no mutan: devuelven un nuevo `record` con copias decrementadas. Esto elimina toda una clase de bugs de estado compartido y combina bien con el flujo `@Transactional`.

### Excepciones de dominio `sealed`

Una única `ExcepcionDominio` `sealed` con 12 subclases `final`. El `ManejadorGlobalExcepciones` hace `switch` exhaustivo: si añades una nueva subclase, el compilador obliga a mapearla a un código HTTP. Errores devueltos como `ProblemDetail` (RFC 7807) en ambos servicios.

### Persistencia con Hibernate (Servicio A) y pgx (Servicio B)

- **A** usa Spring Data JPA con `Specification` para queries dinámicas. Las entidades JPA (`LibroEntidad`, `UsuarioEntidad`) son anémicas a propósito: el modelo rico vive en el dominio, los mapeadores cruzan la frontera.
- **B** usa `pgx v5` directo (sin `database/sql`), aprovechando placeholders nativos `$1`, `$2`, y tipos PostgreSQL. Más rápido, menos abstracciones.

### JWT con dos `SecurityFilterChain`

El Servicio A expone dos superficies de autenticación:

- `/api/v1/internal/**` — `@Order(1)`, autenticada por header `X-Servicio-Interno` con secreto compartido.
- Resto de la API — `@Order(2)`, autenticada por Bearer JWT (HS256).

Cada filtro `@Component` se desactiva globalmente con `FilterRegistrationBean.setEnabled(false)` para evitar doble registro fuera de su cadena.

### Logging JSON estructurado y correlación con `X-Request-Id`

`logback-spring.xml` configura dos appenders con bloques `<springProfile>`:

- **`local | default`** — patrón Spring Boot por defecto, colorizado, legible para desarrollo.
- **`docker`** — `LogstashEncoder` que emite **JSON una línea por evento**, listo para Loki / Fluent Bit / CloudWatch sin re-parseo.

Cada evento incluye `@timestamp` UTC, `level`, `logger_name`, `thread_name`, `message`, `stack_trace` (cuando aplica) y las claves MDC `idSolicitud` e `idUsuario`. Campos estáticos: `servicio` y `entorno` (perfil activo).

**Correlación end-to-end** vía `FiltroIdSolicitud` (`@Order(HIGHEST_PRECEDENCE)`): lee el header `X-Request-Id` entrante (o genera UUID), lo coloca en `MDC.idSolicitud`, lo devuelve en la respuesta y lo limpia en `finally`. El cliente puede pasar su propio trace ID y ver todos los logs del request agrupados — pieza base para tracing distribuido cuando se integre con OpenTelemetry / Jaeger.

**Propagación distribuida A↔B.** El `InterceptorIdSolicitudHttp` (Spring `ClientHttpRequestInterceptor`) lee el `MDC.idSolicitud` actual del Servicio A y lo añade como `X-Request-Id` a cada llamada saliente al Servicio B. El Servicio B (Go) tiene su propio middleware (`middleware.IDSolicitud` + `observabilidad.IDSolicitudDesdeContexto`) que recibe el header, lo guarda en el `context.Context` y lo emite como `request_id` en cada log slog. Lo mismo en sentido inverso: el cliente Go del Servicio B → endpoint interno A propaga el id desde el context. Resultado: **una única solicitud del cliente produce logs correlacionables en ambos servicios con el mismo trace ID**.

```json
{
  "@timestamp": "2026-06-07T23:34:55.890Z",
  "level": "WARN",
  "logger_name": "com.mariposa.biblioteca.infraestructura.seguridad.FiltroLimiteTasaInicioSesion",
  "thread_name": "http-nio-8080-exec-4",
  "message": "Límite de intentos de inicio de sesión excedido para clave=203.0.113.10 reintentarEn=59s",
  "idSolicitud": "smoke-rate-6",
  "servicio": "servicio-biblioteca",
  "entorno": "docker"
}
```

### Rate limiting en `POST /api/v1/autenticacion/iniciar-sesion`

Filtro servlet basado en **Bucket4j 8.19** (`FiltroLimiteTasaInicioSesion`) que limita los intentos de inicio de sesión por IP de cliente (con soporte de `X-Forwarded-For`). Token bucket en memoria, capacidad por defecto 5 intentos por minuto, configurable vía:

```yaml
seguridad:
  limite-tasa:
    inicio-sesion:
      habilitado: true
      capacidad: 5
      ventana-segundos: 60
```

Se ejecuta **antes** del filtro JWT y antes del costoso BCrypt en el caso de uso. Cuando se excede el límite, responde `429 Too Many Requests` con cuerpo `application/problem+json` (RFC 7807), header `Retry-After` con los segundos restantes hasta el refill y `X-Rate-Limit-Remaining: 0`. Mitigación estándar contra fuerza bruta sin afectar a otros endpoints.

### Resilience4j en el cliente HTTP del Servicio A

`ClientePrestamosRest` está envuelto en `@CircuitBreaker(fallbackMethod=...)` + `@Retry`:

- Sólo `5xx` y `ResourceAccessException` (red) reintentan (3 intentos, backoff 500ms).
- `4xx` no reintenta: se propaga inmediatamente.
- Cuando el circuito está abierto, el fallback distingue `CallNotPermittedException` y devuelve `ServicioPrestamosNoDisponible` (HTTP 503).
- Configurable vía `application.yml` (`failureRateThreshold=50%`, `slidingWindowSize=10`, `waitDurationInOpenState=10s`).

### Orden de operaciones en `ServicioRegistroPrestamo`

`@Transactional` que (1) valida el libro y usuario, (2) llama a `libro.prestar()` en memoria, (3) invoca al Servicio B, (4) si éste responde OK, persiste el libro decrementado. Si B falla, la transacción local hace rollback y no queda decremento espurio. Documentado como límite: si B comitea y luego el `save` en A falla, hay inconsistencia residual (solución completa: outbox pattern).

### Validación en el constructor de los `record`

`Libro` y `Usuario` validan invariantes en el `compact constructor`. No hay forma de instanciar un agregado inválido. Esto reemplaza chequeos defensivos esparcidos por la capa de aplicación.

### Inyección de `Clock` para tests deterministas

`ServicioRegistroPrestamo` recibe `Clock`. En producción es `Clock.systemUTC()`; en tests se inyecta un `Clock` fijo para verificar fechas sin congelar el tiempo del sistema.

### Inglés vs español en la capa de adaptadores externos

Spring Boot, Hibernate, JJWT, pgx, etc. exponen APIs en inglés (`@Bean`, `@Transactional`, `Authentication`). Esos identificadores se mantienen tal cual. El resto (dominio, paquetes, métodos, variables, columnas, mensajes de error, logs) es español.

### Docker multi-stage

Ambos servicios usan multi-stage builds para imágenes finales mínimas:

- **A:** `temurin:21-jdk-alpine` (builder) → `temurin:21-jre-alpine` (runtime, usuario `spring`).
- **B:** `golang:1.25-alpine` (builder) → `alpine:3.20` (runtime, binario estático ~35 MB).

Healthchecks integrados con `wget -q -O /dev/null` (GET en lugar de HEAD, porque Spring Security responde 401 a HEAD incluso en endpoints `permitAll`).

## Licencia

MIT — ver archivo [LICENSE](./LICENSE).
