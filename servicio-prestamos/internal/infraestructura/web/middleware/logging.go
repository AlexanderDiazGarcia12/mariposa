package middleware

import (
	"log/slog"
	"net/http"
	"time"
)

type envoltorioRespuesta struct {
	http.ResponseWriter
	estado int
}

func (e *envoltorioRespuesta) WriteHeader(estado int) {
	e.estado = estado
	e.ResponseWriter.WriteHeader(estado)
}

func Logging(siguiente http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		inicio := time.Now()
		envoltorio := &envoltorioRespuesta{ResponseWriter: w, estado: http.StatusOK}
		siguiente.ServeHTTP(envoltorio, r)
		slog.Info("solicitud HTTP",
			"method", r.Method,
			"path", r.URL.Path,
			"status", envoltorio.estado,
			"duracion_ms", time.Since(inicio).Milliseconds(),
			"request_id", IDSolicitudDesdeContexto(r.Context()),
		)
	})
}
