# Servicio Prestamos (Go)

Microservicio B de la prueba tecnica Grupo Mariposa. Gestiona el ciclo de vida de los
prestamos de libros y consulta al Servicio Biblioteca (Servicio A) para validar la
disponibilidad de los libros antes de registrar un prestamo.

## Stack

- Go 1.25+
- `net/http` (stdlib) + ServeMux con METHOD patterns (Go 1.22+)
- `log/slog` JSON estructurado
- PostgreSQL via [`pgx v5`](https://github.com/jackc/pgx) (`pgxpool`)
- Migraciones SQL embebidas con [`golang-migrate`](https://github.com/golang-migrate/migrate) + `embed.FS`
- Tests: stdlib `testing` + `testify` + `testcontainers-go`
- Imagen Docker multi-stage (Alpine, ~35MB)

## Arquitectura

Arquitectura hexagonal (puertos/adaptadores):

```
cmd/servidor/main.go            -> wiring + arranque + graceful shutdown
internal/dominio                -> Prestamo, EstadoPrestamo, errores sentinela
internal/aplicacion             -> casos de uso + puertos (RepositorioPrestamo, ClienteBiblioteca)
internal/infraestructura/
  persistencia                  -> repositorio pgx + migraciones embebidas
  cliente_biblioteca            -> adaptador HTTP al Servicio A
  web                           -> servidor, middleware, controladores, DTOs
internal/configuracion          -> carga de configuracion desde env vars
```

## API REST

| Metodo | Ruta                                       | Descripcion                                 |
|--------|--------------------------------------------|---------------------------------------------|
| POST   | `/api/v1/prestamos`                        | Registrar un prestamo                       |
| GET    | `/api/v1/prestamos`                        | Listar todos los prestamos (paginado)       |
| GET    | `/api/v1/prestamos/{id}`                   | Consultar un prestamo por id                |
| POST   | `/api/v1/prestamos/{id}/devolver`          | Marcar como devuelto                        |
| GET    | `/api/v1/prestamos/usuario/{idUsuario}`    | Listar prestamos de un usuario (paginado)   |
| GET    | `/salud`                                   | Healthcheck                                 |

Los errores se devuelven en formato **ProblemDetail RFC 7807**
(`application/problem+json`).

## Comunicacion con el Servicio Biblioteca

Para validar disponibilidad antes de registrar, el cliente HTTP llama:

```
GET {SERVICIO_BIBLIOTECA_URL}/api/v1/internal/libros/{idLibro}
Header: X-Servicio-Interno: {SERVICIO_INTERNO_SECRETO}
```

> Este endpoint interno se implementara en el Servicio A en una proxima tanda.
> Mientras tanto, el cliente esta listo, los tests usan stubs `httptest`, y los
> errores 401/403/5xx se mapean a `ErrServicioBibliotecaNoDisponible` (HTTP 503).

Politicas implementadas:
- Timeout configurable (`HTTP_CLIENTE_TIMEOUT_SEGUNDOS`, default 5s).
- Reintentos con backoff exponencial (200ms, 500ms) solo en 5xx y errores de red.
- Sin reintentos en 4xx.

## Variables de entorno

| Variable                          | Default                                            |
|-----------------------------------|----------------------------------------------------|
| `SERVICIO_PRESTAMOS_PUERTO`       | `8081`                                             |
| `POSTGRES_PRESTAMOS_HOST`         | `postgres-prestamos`                               |
| `POSTGRES_PRESTAMOS_PORT`         | `5432`                                             |
| `POSTGRES_PRESTAMOS_DB`           | `prestamos`                                        |
| `POSTGRES_PRESTAMOS_USER`         | `prestamos_app`                                    |
| `POSTGRES_PRESTAMOS_PASSWORD`     | `cambiar_en_local`                                 |
| `POSTGRES_PRESTAMOS_SSLMODE`      | `disable`                                          |
| `SERVICIO_PRESTAMOS_DSN`          | (opcional, sobreescribe los anteriores)            |
| `SERVICIO_BIBLIOTECA_URL`         | `http://servicio-biblioteca:8080`                  |
| `SERVICIO_INTERNO_SECRETO`        | `secreto-compartido-mariposa-cambiar-en-produccion`|
| `HTTP_CLIENTE_TIMEOUT_SEGUNDOS`   | `5`                                                |

## Ejecucion local

```bash
# Desde el monorepo:
docker compose up --build servicio-prestamos
```

## Tests

```bash
go test ./...
```

Los tests del repositorio levantan un PostgreSQL via `testcontainers-go`
(requiere Docker en marcha). Para omitirlos:

```bash
OMITIR_TESTS_INTEGRACION=1 go test ./...
```
