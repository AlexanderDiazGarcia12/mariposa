# Pruebas cURL — Sistema Mariposa

Smoke test end-to-end para validar el funcionamiento del sistema distribuido
(Servicio A `biblioteca` + Servicio B `prestamos`) una vez levantado con
`docker compose up`.

Prerrequisitos:

- Contenedores `mariposa-postgres`, `mariposa-postgres-prestamos`,
  `mariposa-servicio-biblioteca` y `mariposa-servicio-prestamos` en estado
  `healthy`. Verifica con `docker compose ps`.
- Servicio A expuesto en `http://localhost:8080`.
- Servicio B expuesto en `http://localhost:8081`.
- `jq` instalado para parsear las respuestas (opcional pero recomendado).

Variables base usadas en los ejemplos:

```bash
export BASE_A="http://localhost:8080"
export BASE_B="http://localhost:8081"
export SERVICIO_INTERNO_SECRETO="secreto-compartido-mariposa-cambiar-en-produccion"
```

> El valor de `SERVICIO_INTERNO_SECRETO` debe coincidir con el de tu archivo
> `.env`.

---

## Índice

- [Parte 1 — Servicio Biblioteca (A)](#parte-1--servicio-biblioteca-a)
  - [1. Login del administrador semilla](#1-login-del-administrador-semilla)
  - [2. Crear libros (requiere rol ADMIN)](#2-crear-libros-requiere-rol-admin)
  - [3. Listar libros con filtros, paginación y ordenamiento](#3-listar-libros-con-filtros-paginación-y-ordenamiento)
  - [4. Registrar usuario público](#4-registrar-usuario-público)
  - [5. Login del usuario recién registrado](#5-login-del-usuario-recién-registrado)
  - [6. Acceso sin autenticación (401)](#6-acceso-sin-autenticación-401)
  - [7. Acceso con rol insuficiente (403)](#7-acceso-con-rol-insuficiente-403)
  - [8. Endpoints útiles adicionales](#8-endpoints-útiles-adicionales)
- [Parte 2 — Flujo distribuido A↔B (Préstamos)](#parte-2--flujo-distribuido-ab-préstamos)
  - [9. Registrar un préstamo (orquesta A→B→A)](#9-registrar-un-préstamo-orquesta-aba)
  - [10. Verificar el decremento de copias en A](#10-verificar-el-decremento-de-copias-en-a)
  - [11. Consultar el préstamo en el Servicio B](#11-consultar-el-préstamo-en-el-servicio-b)
  - [12. Devolver el préstamo (Servicio B)](#12-devolver-el-préstamo-servicio-b)
  - [13. Listar préstamos por usuario (Servicio B)](#13-listar-préstamos-por-usuario-servicio-b)
- [Parte 3 — Endpoint interno M2M y casos de error](#parte-3--endpoint-interno-m2m-y-casos-de-error)
  - [14. Endpoint interno con y sin header secreto](#14-endpoint-interno-con-y-sin-header-secreto)
  - [15. Préstamo con libro sin copias (409)](#15-préstamo-con-libro-sin-copias-409)
  - [16. Préstamo con fechas inválidas (400)](#16-préstamo-con-fechas-inválidas-400)
  - [17. Servicio B caído (503 vía circuit breaker)](#17-servicio-b-caído-503-vía-circuit-breaker)
  - [18. Rate limiting en `/iniciar-sesion` (429)](#18-rate-limiting-en-iniciar-sesion-429)
  - [19. Logs JSON estructurados y `X-Request-Id`](#19-logs-json-estructurados-y-x-request-id)
  - [20. Trace ID propagado A↔B (correlación distribuida)](#20-trace-id-propagado-ab-correlación-distribuida)

---

# Parte 1 — Servicio Biblioteca (A)

## 1. Login del administrador semilla

Credenciales por defecto:

- nombre de usuario: `admin`
- contraseña: `Admin123!`

```bash
curl -sS -X POST "$BASE_A/api/v1/autenticacion/iniciar-sesion" \
  -H "Content-Type: application/json" \
  -d '{
    "nombreUsuario": "admin",
    "contrasena": "Admin123!"
  }'
```

Extraer el `tokenAcceso` en una variable de entorno:

```bash
export TOKEN=$(curl -sS -X POST "$BASE_A/api/v1/autenticacion/iniciar-sesion" \
  -H "Content-Type: application/json" \
  -d '{"nombreUsuario":"admin","contrasena":"Admin123!"}' \
  | jq -r '.tokenAcceso')

echo "TOKEN: $TOKEN"
```

Respuesta esperada (200 OK):

```json
{
  "tokenAcceso": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenRefresco": "eyJhbGciOiJIUzI1NiJ9...",
  "expiraEn": "2026-06-06T12:30:00Z",
  "usuario": {
    "id": "00000000-0000-0000-0000-000000000001",
    "nombreUsuario": "admin",
    "correoElectronico": "admin@mariposa.local",
    "rol": "ADMIN",
    "estado": "ACTIVO"
  }
}
```

---

## 2. Crear libros (requiere rol ADMIN)

```bash
export ID_LIBRO=$(curl -sS -X POST "$BASE_A/api/v1/libros" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "titulo": "Cien anios de soledad",
    "autor": "Gabriel Garcia Marquez",
    "isbn": "9780307474728",
    "anioPublicacion": 1967,
    "genero": "FICCION",
    "copiasTotales": 5
  }' | jq -r '.id')

echo "ID_LIBRO: $ID_LIBRO"
```

Respuesta esperada: `201 Created` con la representación del libro y header `Location`.

Crear un segundo libro para tener datos al listar:

```bash
curl -sS -X POST "$BASE_A/api/v1/libros" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "titulo": "Sapiens",
    "autor": "Yuval Noah Harari",
    "isbn": "9788499926223",
    "anioPublicacion": 2011,
    "genero": "NO_FICCION",
    "copiasTotales": 3
  }'
```

---

## 3. Listar libros con filtros, paginación y ordenamiento

Listado simple (endpoint público, no requiere token):

```bash
curl -sS "$BASE_A/api/v1/libros?pagina=0&tamano=20&orden=POR_TITULO_ASC"
```

Filtrar por autor y disponibilidad:

```bash
curl -sS \
  --get "$BASE_A/api/v1/libros" \
  --data-urlencode "autor=Gabriel" \
  --data-urlencode "conDisponibilidad=true" \
  --data-urlencode "pagina=0" \
  --data-urlencode "tamano=10" \
  --data-urlencode "orden=POR_TITULO_ASC"
```

Filtrar por género:

```bash
curl -sS "$BASE_A/api/v1/libros?genero=FICCION&pagina=0&tamano=10"
```

Buscar por ISBN:

```bash
curl -sS "$BASE_A/api/v1/libros/por-isbn/9780307474728"
```

---

## 4. Registrar usuario público

```bash
curl -sS -X POST "$BASE_A/api/v1/usuarios" \
  -H "Content-Type: application/json" \
  -d '{
    "nombreUsuario": "lector01",
    "correoElectronico": "lector01@mariposa.local",
    "contrasena": "Lector123!",
    "rol": "USUARIO"
  }'
```

Respuesta esperada: `201 Created` con header `Location: /api/v1/usuarios/{id}`.

---

## 5. Login del usuario recién registrado

```bash
export TOKEN_USUARIO=$(curl -sS -X POST "$BASE_A/api/v1/autenticacion/iniciar-sesion" \
  -H "Content-Type: application/json" \
  -d '{"nombreUsuario":"lector01","contrasena":"Lector123!"}' \
  | jq -r '.tokenAcceso')

export ID_USUARIO=$(curl -sS -X POST "$BASE_A/api/v1/autenticacion/iniciar-sesion" \
  -H "Content-Type: application/json" \
  -d '{"nombreUsuario":"lector01","contrasena":"Lector123!"}' \
  | jq -r '.usuario.id')

echo "TOKEN_USUARIO: $TOKEN_USUARIO"
echo "ID_USUARIO:    $ID_USUARIO"
```

---

## 6. Acceso sin autenticación (401)

Intentar crear un libro sin token:

```bash
curl -sS -o /dev/null -w "HTTP %{http_code}\n" \
  -X POST "$BASE_A/api/v1/libros" \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Sin auth",
    "autor": "Anonimo",
    "isbn": "9999999999999",
    "anioPublicacion": 2020,
    "genero": "OTRO",
    "copiasTotales": 1
  }'
```

Respuesta esperada: `HTTP 401`.

Listar usuarios sin token (endpoint protegido):

```bash
curl -sS -o /dev/null -w "HTTP %{http_code}\n" "$BASE_A/api/v1/usuarios"
```

Respuesta esperada: `HTTP 401`.

---

## 7. Acceso con rol insuficiente (403)

Intentar crear un libro como usuario regular (rol `USUARIO` en lugar de `ADMIN`):

```bash
curl -sS -o /dev/null -w "HTTP %{http_code}\n" \
  -X POST "$BASE_A/api/v1/libros" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_USUARIO" \
  -d '{
    "titulo": "Sin permiso",
    "autor": "Lector regular",
    "isbn": "8888888888888",
    "anioPublicacion": 2020,
    "genero": "OTRO",
    "copiasTotales": 1
  }'
```

Respuesta esperada: `HTTP 403`.

Listar todos los usuarios siendo lector (requiere ADMIN):

```bash
curl -sS -o /dev/null -w "HTTP %{http_code}\n" \
  -H "Authorization: Bearer $TOKEN_USUARIO" \
  "$BASE_A/api/v1/usuarios"
```

Respuesta esperada: `HTTP 403`.

---

## 8. Endpoints útiles adicionales

Health check:

```bash
curl -sS "$BASE_A/actuator/health"
```

Documentación OpenAPI:

```bash
curl -sS "$BASE_A/v3/api-docs"
```

Swagger UI (abrir en el navegador): `http://localhost:8080/swagger-ui.html`

Refrescar token:

```bash
curl -sS -X POST "$BASE_A/api/v1/autenticacion/refrescar" \
  -H "Content-Type: application/json" \
  -d "{\"tokenRefresco\":\"$TOKEN_REFRESCO\"}"
```

---

# Parte 2 — Flujo distribuido A↔B (Préstamos)

Este es el flujo principal de la prueba: un usuario autenticado solicita un
préstamo en el Servicio A, que valida localmente y delega la persistencia al
Servicio B. Las copias del libro en el Servicio A se decrementan sólo si el
Servicio B confirma el registro.

```
Cliente ─Bearer JWT─▶ POST /api/v1/prestamos (A)
                          │
                          ├─ valida usuario + libro + copias
                          ├─ libro.prestar() en memoria
                          ├─ POST /api/v1/prestamos (B) ──┐
                          │                                │
                          │       (envuelto en             │
                          │        @CircuitBreaker         │
                          │        + @Retry)               │
                          │                                ▼
                          │            persiste préstamo en BD prestamos
                          │                                │
                          │ ◀─── 201 PrestamoRespuestaB ───┘
                          │
                          ├─ guarda libro con copias decrementadas
                          ▼
              201 PrestamoRespuesta
```

## 9. Registrar un préstamo (orquesta A→B→A)

Asegúrate de tener exportadas las variables `TOKEN_USUARIO` (paso 5) e
`ID_LIBRO` (paso 2). Las fechas usan formato ISO `yyyy-MM-dd` y deben ser hoy o
posteriores (`@FutureOrPresent`):

```bash
export HOY=$(date -u +%Y-%m-%d)
# +14 días en macOS / BSD:
export EN_DOS_SEMANAS=$(date -u -v+14d +%Y-%m-%d)
# En Linux usa: export EN_DOS_SEMANAS=$(date -u -d "+14 days" +%Y-%m-%d)

curl -sS -X POST "$BASE_A/api/v1/prestamos" \
  -H "Authorization: Bearer $TOKEN_USUARIO" \
  -H "Content-Type: application/json" \
  -d "{
        \"idLibro\":\"$ID_LIBRO\",
        \"fechaPrestamo\":\"$HOY\",
        \"fechaDevolucionEstimada\":\"$EN_DOS_SEMANAS\"
      }" | jq
```

Respuesta esperada (`201 Created`, header `Location: /api/v1/prestamos/{idPrestamo}`):

```json
{
  "idPrestamo": "771010cf-de8c-468d-8a93-81bef06dc421",
  "idUsuario": "aaaaaaaa-1111-2222-3333-cccccccccccc",
  "idLibro": "1f4a2c8d-aaaa-4bcd-9012-1234567890ab",
  "fechaPrestamo": "2026-06-07",
  "fechaDevolucionEstimada": "2026-06-21",
  "estado": "ACTIVO",
  "registradoEn": "2026-06-07T10:15:00Z"
}
```

Extrae el ID del préstamo para los pasos siguientes:

```bash
export ID_PRESTAMO=$(curl -sS -X POST "$BASE_A/api/v1/prestamos" \
  -H "Authorization: Bearer $TOKEN_USUARIO" \
  -H "Content-Type: application/json" \
  -d "{
        \"idLibro\":\"$ID_LIBRO\",
        \"fechaPrestamo\":\"$HOY\",
        \"fechaDevolucionEstimada\":\"$EN_DOS_SEMANAS\"
      }" | jq -r '.idPrestamo')

echo "ID_PRESTAMO: $ID_PRESTAMO"
```

---

## 10. Verificar el decremento de copias en A

El libro originalmente tenía 5 copias. Tras 1 préstamo deben quedar 4:

```bash
curl -sS -H "Authorization: Bearer $TOKEN_USUARIO" \
  "$BASE_A/api/v1/libros/$ID_LIBRO" | jq '{titulo, copiasTotales, copiasDisponibles}'
```

Respuesta esperada:

```json
{
  "titulo": "Cien anios de soledad",
  "copiasTotales": 5,
  "copiasDisponibles": 4
}
```

---

## 11. Consultar el préstamo en el Servicio B

El Servicio B almacena los préstamos en su propia base de datos:

```bash
curl -sS "$BASE_B/api/v1/prestamos/$ID_PRESTAMO" | jq
```

Respuesta esperada:

```json
{
  "id": "771010cf-de8c-468d-8a93-81bef06dc421",
  "idUsuario": "aaaaaaaa-1111-2222-3333-cccccccccccc",
  "idLibro": "1f4a2c8d-aaaa-4bcd-9012-1234567890ab",
  "fechaPrestamo": "2026-06-07",
  "fechaDevolucionEstimada": "2026-06-21",
  "estado": "ACTIVO",
  "estaAtrasado": false,
  "fechaCreacion": "2026-06-07T10:15:00Z",
  "fechaActualizacion": "2026-06-07T10:15:00Z"
}
```

Listar todos los préstamos paginados:

```bash
curl -sS "$BASE_B/api/v1/prestamos?pagina=0&tamano=20" | jq
```

---

## 12. Devolver el préstamo (Servicio B)

```bash
curl -sS -X POST "$BASE_B/api/v1/prestamos/$ID_PRESTAMO/devolver" | jq
```

Respuesta esperada (`200 OK`):

```json
{
  "id": "771010cf-de8c-468d-8a93-81bef06dc421",
  "idUsuario": "aaaaaaaa-1111-2222-3333-cccccccccccc",
  "idLibro": "1f4a2c8d-aaaa-4bcd-9012-1234567890ab",
  "fechaPrestamo": "2026-06-07",
  "fechaDevolucionEstimada": "2026-06-21",
  "fechaDevolucionReal": "2026-06-07",
  "estado": "DEVUELTO",
  "estaAtrasado": false,
  "fechaCreacion": "2026-06-07T10:15:00Z",
  "fechaActualizacion": "2026-06-07T10:20:00Z"
}
```

> Nota: la devolución se procesa en el Servicio B. La reposición de la copia
> disponible en el Servicio A no se sincroniza automáticamente en esta versión
> (queda como mejora futura — outbox pattern o callback B→A). Este es un límite
> consciente del diseño y se documenta en el README.

---

## 13. Listar préstamos por usuario (Servicio B)

```bash
curl -sS "$BASE_B/api/v1/prestamos/usuario/$ID_USUARIO?pagina=0&tamano=20" | jq
```

Respuesta esperada (`200 OK`): página de préstamos del usuario, ordenados por
`fechaCreacion` descendente.

---

# Parte 3 — Endpoint interno M2M y casos de error

## 14. Endpoint interno con y sin header secreto

El endpoint `GET /api/v1/internal/libros/{id}` está reservado para comunicación
service-to-service. Es el endpoint que invoca el Servicio B para validar
disponibilidad. Está protegido por un `SecurityFilterChain` con `@Order(1)` que
verifica el header `X-Servicio-Interno`.

Sin el header → `401 Unauthorized`:

```bash
curl -sS -o /dev/null -w "HTTP %{http_code}\n" \
  "$BASE_A/api/v1/internal/libros/$ID_LIBRO"
```

Resultado esperado: `HTTP 401`.

Con el header correcto → `200 OK`:

```bash
curl -sS -H "X-Servicio-Interno: $SERVICIO_INTERNO_SECRETO" \
  "$BASE_A/api/v1/internal/libros/$ID_LIBRO" | jq
```

Con un secreto inválido → `401 Unauthorized`:

```bash
curl -sS -o /dev/null -w "HTTP %{http_code}\n" \
  -H "X-Servicio-Interno: secreto-incorrecto" \
  "$BASE_A/api/v1/internal/libros/$ID_LIBRO"
```

Resultado esperado: `HTTP 401`.

> Importante: usar el JWT (Bearer) en este endpoint también devuelve 401,
> porque está fuera del filter chain JWT. Cada cadena tiene su propia
> autenticación.

---

## 15. Préstamo con libro sin copias (409)

Crear un libro con 1 sola copia y agotarla:

```bash
export ID_LIBRO_ESCASO=$(curl -sS -X POST "$BASE_A/api/v1/libros" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "titulo": "Libro escaso",
    "autor": "Autor",
    "isbn": "1234567890123",
    "anioPublicacion": 2020,
    "genero": "OTRO",
    "copiasTotales": 1
  }' | jq -r '.id')

# Primer préstamo: 201 OK
curl -sS -X POST "$BASE_A/api/v1/prestamos" \
  -H "Authorization: Bearer $TOKEN_USUARIO" \
  -H "Content-Type: application/json" \
  -d "{
        \"idLibro\":\"$ID_LIBRO_ESCASO\",
        \"fechaPrestamo\":\"$HOY\",
        \"fechaDevolucionEstimada\":\"$EN_DOS_SEMANAS\"
      }" | jq

# Segundo préstamo: 409 Conflict
curl -sS -X POST "$BASE_A/api/v1/prestamos" \
  -H "Authorization: Bearer $TOKEN_USUARIO" \
  -H "Content-Type: application/json" \
  -d "{
        \"idLibro\":\"$ID_LIBRO_ESCASO\",
        \"fechaPrestamo\":\"$HOY\",
        \"fechaDevolucionEstimada\":\"$EN_DOS_SEMANAS\"
      }" | jq
```

Respuesta esperada del segundo préstamo (`409 Conflict`, RFC 7807):

```json
{
  "type": "https://mariposa.local/errores/sin-copias-disponibles",
  "title": "Sin copias disponibles",
  "status": 409,
  "detail": "El libro no tiene copias disponibles para prestar.",
  "instance": "/api/v1/prestamos"
}
```

---

## 16. Préstamo con fechas inválidas (400)

`fechaPrestamo` en el pasado falla la validación `@FutureOrPresent`:

```bash
curl -sS -o /dev/null -w "HTTP %{http_code}\n" \
  -X POST "$BASE_A/api/v1/prestamos" \
  -H "Authorization: Bearer $TOKEN_USUARIO" \
  -H "Content-Type: application/json" \
  -d "{
        \"idLibro\":\"$ID_LIBRO\",
        \"fechaPrestamo\":\"2020-01-01\",
        \"fechaDevolucionEstimada\":\"$EN_DOS_SEMANAS\"
      }"
```

Resultado esperado: `HTTP 400`.

`idLibro` ausente:

```bash
curl -sS -X POST "$BASE_A/api/v1/prestamos" \
  -H "Authorization: Bearer $TOKEN_USUARIO" \
  -H "Content-Type: application/json" \
  -d "{
        \"fechaPrestamo\":\"$HOY\",
        \"fechaDevolucionEstimada\":\"$EN_DOS_SEMANAS\"
      }" | jq
```

Respuesta esperada (`400 Bad Request`, RFC 7807) listando los campos
inválidos en `errores`.

---

## 17. Servicio B caído (503 vía circuit breaker)

Simular caída del Servicio B y observar el manejo del circuit breaker en A:

```bash
docker compose stop servicio-prestamos
```

Intentar registrar un préstamo:

```bash
curl -sS -o /dev/null -w "HTTP %{http_code}\n" \
  -X POST "$BASE_A/api/v1/prestamos" \
  -H "Authorization: Bearer $TOKEN_USUARIO" \
  -H "Content-Type: application/json" \
  -d "{
        \"idLibro\":\"$ID_LIBRO\",
        \"fechaPrestamo\":\"$HOY\",
        \"fechaDevolucionEstimada\":\"$EN_DOS_SEMANAS\"
      }"
```

Resultado esperado tras los reintentos: `HTTP 503` con cuerpo RFC 7807:

```json
{
  "type": "https://mariposa.local/errores/servicio-prestamos-no-disponible",
  "title": "Servicio de préstamos no disponible",
  "status": 503,
  "detail": "El servicio de préstamos no está disponible en este momento. Inténtalo más tarde."
}
```

Tras 5 fallos consecutivos en una ventana de 10 llamadas el circuito se
**abre** (`OPEN`). Mientras esté abierto, las llamadas fallan rápido sin
intentar contactar a B (`CallNotPermittedException` → 503 inmediato). Tras
10 s pasa a `HALF_OPEN` y permite llamadas de prueba.

Para volver a la normalidad:

```bash
docker compose start servicio-prestamos
# Esperar a que esté healthy:
docker compose ps
```

Repetir el POST de préstamo: una vez B esté arriba y se ejecuten algunas
llamadas exitosas, el circuito vuelve a `CLOSED`.

---

## 18. Rate limiting en `/iniciar-sesion` (429)

El endpoint `POST /api/v1/autenticacion/iniciar-sesion` está protegido por un
filtro **Bucket4j** (token bucket en memoria) que limita los intentos por IP a
**5 cada 60 segundos** por defecto (configurable vía `LIMITE_TASA_INICIO_SESION_*`).
La clave es la IP del cliente, con soporte de `X-Forwarded-For` para proxies.

Disparar 6 intentos seguidos (los 5 primeros entran al sistema; el 6° se rechaza
antes del BCrypt):

```bash
for i in $(seq 1 6); do
  echo -n "Intento $i: "
  curl -sS -o /dev/null \
    -w "HTTP %{http_code} | Remaining=%header{x-rate-limit-remaining} | Retry-After=%header{Retry-After}\n" \
    -X POST "$BASE_A/api/v1/autenticacion/iniciar-sesion" \
    -H "Content-Type: application/json" \
    -d '{"nombreUsuario":"admin","contrasena":"contrasena-incorrecta"}'
done
```

Salida esperada:

```
Intento 1: HTTP 401 | Remaining=4 | Retry-After=
Intento 2: HTTP 401 | Remaining=3 | Retry-After=
Intento 3: HTTP 401 | Remaining=2 | Retry-After=
Intento 4: HTTP 401 | Remaining=1 | Retry-After=
Intento 5: HTTP 401 | Remaining=0 | Retry-After=
Intento 6: HTTP 429 | Remaining=0 | Retry-After=59
```

Cuerpo de la respuesta `429` (RFC 7807):

```bash
curl -sS -X POST "$BASE_A/api/v1/autenticacion/iniciar-sesion" \
  -H "Content-Type: application/json" \
  -d '{"nombreUsuario":"admin","contrasena":"x"}' | jq
```

```json
{
  "type": "urn:problema:limite-tasa-excedido",
  "title": "Límite de tasa excedido",
  "status": 429,
  "detail": "Demasiados intentos de inicio de sesión. Inténtalo más tarde.",
  "instance": "/api/v1/autenticacion/iniciar-sesion",
  "properties": {
    "reintentarEnSegundos": 59
  }
}
```

Verificar que los buckets están segregados por IP (cada cliente tiene su propia
cuota):

```bash
curl -sS -o /dev/null \
  -w "HTTP %{http_code} | Remaining=%header{x-rate-limit-remaining}\n" \
  -X POST "$BASE_A/api/v1/autenticacion/iniciar-sesion" \
  -H "Content-Type: application/json" \
  -H "X-Forwarded-For: 203.0.113.99" \
  -d '{"nombreUsuario":"admin","contrasena":"Admin123!"}'
```

Resultado esperado: `HTTP 200 | Remaining=4` (la IP virtual `203.0.113.99` tiene
su bucket fresco aunque la IP original esté bloqueada).

> El filtro corre **antes** del filtro JWT y antes del costoso BCrypt del caso
> de uso. Esto significa que cuando la cuota se agota, las llamadas se rechazan
> rápido (< 1 ms) sin tocar la base de datos.

> Importante: el rate limit aplica **sólo** a `POST /api/v1/autenticacion/iniciar-sesion`.
> El resto de endpoints (incluido `/refrescar`, `/actuator/health`, `/api/v1/libros`,
> etc.) no está afectado.

---

## 19. Logs JSON estructurados y `X-Request-Id`

En perfil `docker` (el que activa `docker-compose.yml`), el Servicio A emite logs
JSON una línea por evento. Cada request gana automáticamente un `idSolicitud`
en MDC, devuelto al cliente como header `X-Request-Id`. Si el cliente envía su
propio `X-Request-Id`, el filtro lo preserva (truncado a 128 caracteres).

Hacer un request enviando un trace ID propio y verificar que regresa:

```bash
curl -sS -o /dev/null \
  -w "X-Request-Id devuelto: %header{X-Request-Id}\n" \
  -H "X-Request-Id: mi-trace-12345" \
  -X POST "$BASE_A/api/v1/autenticacion/iniciar-sesion" \
  -H "Content-Type: application/json" \
  -d '{"nombreUsuario":"admin","contrasena":"x"}'
```

Salida:

```
X-Request-Id devuelto: mi-trace-12345
```

Sin enviar el header, el servidor genera un UUID:

```bash
curl -sS -o /dev/null \
  -w "X-Request-Id generado: %header{X-Request-Id}\n" \
  "$BASE_A/actuator/health"
```

Salida ejemplo:

```
X-Request-Id generado: 01b89d3f-4e09-4a50-af3a-1446432ca564
```

Inspeccionar los logs del contenedor — son JSON puro, parseable directamente:

```bash
docker logs mariposa-servicio-biblioteca 2>&1 \
  | grep "mi-trace-12345" \
  | jq .
```

Ejemplo de evento (un fallo de contraseña):

```json
{
  "@timestamp": "2026-06-07T23:34:55.890Z",
  "@version": "1",
  "message": "Autenticación rechazada: contraseña no coincide",
  "logger_name": "com.mariposa.biblioteca.aplicacion.servicios.ServicioAutenticacion",
  "thread_name": "http-nio-8080-exec-4",
  "level": "DEBUG",
  "level_value": 10000,
  "idSolicitud": "mi-trace-12345",
  "servicio": "servicio-biblioteca",
  "entorno": "docker"
}
```

Filtrar todos los eventos de una solicitud específica (correlación):

```bash
docker logs mariposa-servicio-biblioteca 2>&1 \
  | jq -c 'select(.idSolicitud == "mi-trace-12345")'
```

> Cuando se integre con Loki / Fluent Bit / CloudWatch, esta estructura no
> necesita parsing adicional — `idSolicitud`, `level` y `logger_name` quedan
> indexados automáticamente.

---

## 20. Trace ID propagado A↔B (correlación distribuida)

El header `X-Request-Id` viaja desde el cliente al Servicio A, y de éste al
Servicio B en la llamada interna del registro de préstamo. Una sola solicitud
del cliente produce logs en ambos servicios con el mismo trace ID — base de
observabilidad distribuida.

Necesitas haber ejecutado los pasos 1, 2 y 5 (tener `TOKEN_USUARIO` o `TOKEN`,
e `ID_LIBRO`). Registra un préstamo con tu propio trace ID:

```bash
TRACE="trace-e2e-$(date +%s)"
echo "TRACE = $TRACE"

curl -sS -o /dev/null \
  -w "HTTP %{http_code} | X-Request-Id devuelto: %header{X-Request-Id}\n" \
  -X POST "$BASE_A/api/v1/prestamos" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Request-Id: $TRACE" \
  -H "Content-Type: application/json" \
  -d "{
        \"idLibro\":\"$ID_LIBRO\",
        \"fechaPrestamo\":\"$HOY\",
        \"fechaDevolucionEstimada\":\"$EN_DOS_SEMANAS\"
      }"
```

Salida esperada: `HTTP 201 | X-Request-Id devuelto: trace-e2e-XXXX`.

Buscar el trace en logs del **Servicio A**:

```bash
docker logs mariposa-servicio-biblioteca 2>&1 | grep "$TRACE" | jq .
```

Salida ejemplo:

```json
{
  "@timestamp": "2026-06-08T03:30:00.123Z",
  "level": "INFO",
  "logger_name": "com.mariposa.biblioteca.aplicacion.servicios.ServicioRegistroPrestamo",
  "message": "Préstamo 932f8d7b-... registrado para usuario ...",
  "idSolicitud": "trace-e2e-1780889995",
  "servicio": "servicio-biblioteca",
  "entorno": "docker"
}
```

Buscar **el mismo trace** en logs del **Servicio B**:

```bash
docker logs mariposa-servicio-prestamos 2>&1 | grep "$TRACE" | jq .
```

Salida ejemplo:

```json
{
  "time": "2026-06-08T03:30:00.456Z",
  "level": "INFO",
  "msg": "solicitud HTTP",
  "method": "POST",
  "path": "/api/v1/prestamos",
  "status": 201,
  "duracion_ms": 23,
  "request_id": "trace-e2e-1780889995"
}
```

> El mismo `trace-e2e-1780889995` aparece en logs JSON de **ambos servicios**,
> aunque cada uno tenga su propio runtime (Java/Go), su propio logger
> (Logback/slog) y su propio formato de campos (`idSolicitud` vs `request_id`).
> Eso es correlación distribuida.

Cuando el cliente NO envía el header, el Servicio A genera un UUID y lo propaga
de la misma forma:

```bash
curl -sS -o /dev/null \
  -w "X-Request-Id generado: %header{X-Request-Id}\n" \
  -X POST "$BASE_A/api/v1/prestamos" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
        \"idLibro\":\"$ID_LIBRO\",
        \"fechaPrestamo\":\"$HOY\",
        \"fechaDevolucionEstimada\":\"$EN_DOS_SEMANAS\"
      }"
```

Salida:

```
X-Request-Id generado: 01b89d3f-4e09-4a50-af3a-1446432ca564
```

---

## Apéndice — Reset rápido de variables

Si necesitas reiniciar el flujo desde cero:

```bash
unset TOKEN TOKEN_USUARIO ID_USUARIO ID_LIBRO ID_LIBRO_ESCASO ID_PRESTAMO HOY EN_DOS_SEMANAS

export BASE_A="http://localhost:8080"
export BASE_B="http://localhost:8081"
export SERVICIO_INTERNO_SECRETO="secreto-compartido-mariposa-cambiar-en-produccion"
```
