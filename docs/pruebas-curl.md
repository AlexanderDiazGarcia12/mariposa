# Pruebas cURL - Servicio Biblioteca

Smoke test end-to-end para validar el funcionamiento del Servicio A (biblioteca)
una vez levantado con `docker compose up`.

Prerrequisitos:
- Contenedores `mariposa-postgres` y `mariposa-servicio-biblioteca` en estado `healthy`.
- Servicio expuesto en `http://localhost:8080`.
- `jq` instalado para parsear las respuestas (opcional pero recomendado).

Variables base usadas en los ejemplos:

```bash
export BASE_URL="http://localhost:8080"
```

---

## 1. Login del usuario administrador semilla

Credenciales por defecto:
- nombre de usuario: `admin`
- contrasena: `Admin123!`

```bash
curl -sS -X POST "$BASE_URL/api/v1/autenticacion/iniciar-sesion" \
  -H "Content-Type: application/json" \
  -d '{
    "nombreUsuario": "admin",
    "contrasena": "Admin123!"
  }'
```

Extraer el `tokenAcceso` en una variable de entorno:

```bash
export TOKEN=$(curl -sS -X POST "$BASE_URL/api/v1/autenticacion/iniciar-sesion" \
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

## 2. Crear un libro (requiere rol ADMIN)

```bash
curl -sS -X POST "$BASE_URL/api/v1/libros" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "titulo": "Cien anios de soledad",
    "autor": "Gabriel Garcia Marquez",
    "isbn": "9780307474728",
    "anioPublicacion": 1967,
    "genero": "FICCION",
    "copiasTotales": 5
  }'
```

Respuesta esperada: `201 Created` con la representación del libro y header `Location`.

Crear un segundo libro para tener datos al listar:

```bash
curl -sS -X POST "$BASE_URL/api/v1/libros" \
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

Listado simple (endpoint publico, no requiere token):

```bash
curl -sS "$BASE_URL/api/v1/libros?pagina=0&tamano=20&orden=POR_TITULO_ASC"
```

Filtrar por autor y disponibilidad:

```bash
curl -sS \
  --get "$BASE_URL/api/v1/libros" \
  --data-urlencode "autor=Gabriel" \
  --data-urlencode "conDisponibilidad=true" \
  --data-urlencode "pagina=0" \
  --data-urlencode "tamano=10" \
  --data-urlencode "orden=POR_TITULO_ASC"
```

Filtrar por genero:

```bash
curl -sS "$BASE_URL/api/v1/libros?genero=FICCION&pagina=0&tamano=10"
```

Buscar por ISBN:

```bash
curl -sS "$BASE_URL/api/v1/libros/por-isbn/9780307474728"
```

---

## 4. Registrar usuario publico

```bash
curl -sS -X POST "$BASE_URL/api/v1/usuarios" \
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

## 5. Login del usuario recien registrado

```bash
export TOKEN_USUARIO=$(curl -sS -X POST "$BASE_URL/api/v1/autenticacion/iniciar-sesion" \
  -H "Content-Type: application/json" \
  -d '{"nombreUsuario":"lector01","contrasena":"Lector123!"}' \
  | jq -r '.tokenAcceso')

echo "TOKEN_USUARIO: $TOKEN_USUARIO"
```

---

## 6. Acceso sin autenticacion (debe devolver 401)

Intentar crear un libro sin token:

```bash
curl -sS -o /dev/null -w "HTTP %{http_code}\n" \
  -X POST "$BASE_URL/api/v1/libros" \
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
curl -sS -o /dev/null -w "HTTP %{http_code}\n" "$BASE_URL/api/v1/usuarios"
```

Respuesta esperada: `HTTP 401`.

---

## 7. Acceso con rol insuficiente (debe devolver 403)

Intentar crear un libro como usuario regular (rol `USUARIO` en lugar de `ADMIN`):

```bash
curl -sS -o /dev/null -w "HTTP %{http_code}\n" \
  -X POST "$BASE_URL/api/v1/libros" \
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
  "$BASE_URL/api/v1/usuarios"
```

Respuesta esperada: `HTTP 403`.

---

## 8. Endpoints utiles adicionales

Health check:

```bash
curl -sS "$BASE_URL/actuator/health"
```

Documentacion OpenAPI:

```bash
curl -sS "$BASE_URL/v3/api-docs"
```

Swagger UI (abrir en el navegador): `http://localhost:8080/swagger-ui.html`

Refrescar token:

```bash
curl -sS -X POST "$BASE_URL/api/v1/autenticacion/refrescar" \
  -H "Content-Type: application/json" \
  -d "{\"tokenRefresco\":\"$TOKEN_REFRESCO\"}"
```
