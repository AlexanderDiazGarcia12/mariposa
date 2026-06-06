# syntax=docker/dockerfile:1.7
# Etapa de construccion: compila el binario estatico.
FROM golang:1.25-alpine AS builder
WORKDIR /build

# Cache de dependencias.
COPY go.mod go.sum ./
RUN go mod download

# Copia el resto del codigo y compila.
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-s -w" -o /build/servicio-prestamos ./cmd/servidor

# Etapa de runtime: imagen minima con el binario.
FROM alpine:3.20 AS runtime
RUN apk add --no-cache ca-certificates wget && \
    addgroup -S servicio && adduser -S servicio -G servicio
WORKDIR /app
COPY --from=builder /build/servicio-prestamos /app/servicio-prestamos
USER servicio:servicio
EXPOSE 8081
ENV SERVICIO_PRESTAMOS_PUERTO=8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -q -O /dev/null http://localhost:8081/salud || exit 1
ENTRYPOINT ["/app/servicio-prestamos"]
