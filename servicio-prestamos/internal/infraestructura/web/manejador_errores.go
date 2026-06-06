package web

import (
	"encoding/json"
	"errors"
	"log/slog"
	"net/http"

	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/dominio"
	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/infraestructura/web/controladores"
	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/infraestructura/web/middleware"
)

func ManejarError(w http.ResponseWriter, r *http.Request, err error) {
	var (
		estado int
		titulo string
		codigo string
	)
	switch {
	case errors.Is(err, dominio.ErrPrestamoNoEncontrado):
		estado, titulo, codigo = http.StatusNotFound, "Prestamo no encontrado", "prestamo-no-encontrado"
	case errors.Is(err, dominio.ErrPrestamoYaDevuelto):
		estado, titulo, codigo = http.StatusConflict, "Prestamo ya devuelto", "prestamo-ya-devuelto"
	case errors.Is(err, dominio.ErrLibroNoEncontrado):
		estado, titulo, codigo = http.StatusUnprocessableEntity, "Libro no encontrado en servicio biblioteca", "libro-no-encontrado"
	case errors.Is(err, dominio.ErrSinCopiasDisponibles):
		estado, titulo, codigo = http.StatusConflict, "Sin copias disponibles", "sin-copias-disponibles"
	case errors.Is(err, dominio.ErrServicioBibliotecaNoDisponible):
		estado, titulo, codigo = http.StatusServiceUnavailable, "Servicio biblioteca no disponible", "servicio-no-disponible"
	case errors.Is(err, dominio.ErrFechaDevolucionInvalida), errors.Is(err, dominio.ErrIDsInvalidos), errors.Is(err, controladores.ErrSolicitudInvalida):
		estado, titulo, codigo = http.StatusUnprocessableEntity, "Datos invalidos", "valor-invalido"
	default:
		estado, titulo, codigo = http.StatusInternalServerError, "Error interno", "error-interno"
		slog.Error("error no manejado",
			"error", err.Error(),
			"path", r.URL.Path,
			"request_id", middleware.IDSolicitudDesdeContexto(r.Context()),
		)
	}

	cuerpo := map[string]any{
		"type":     "https://mariposa.local/problemas/" + codigo,
		"title":    titulo,
		"status":   estado,
		"detail":   err.Error(),
		"instance": r.URL.Path,
	}
	w.Header().Set("Content-Type", "application/problem+json")
	w.WriteHeader(estado)
	_ = json.NewEncoder(w).Encode(cuerpo)
}
