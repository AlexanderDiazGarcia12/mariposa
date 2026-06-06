package middleware

import (
	"encoding/json"
	"log/slog"
	"net/http"
	"runtime/debug"
)

func Recuperacion(siguiente http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		defer func() {
			if rec := recover(); rec != nil {
				slog.Error("panic recuperado",
					"panic", rec,
					"stack", string(debug.Stack()),
					"request_id", IDSolicitudDesdeContexto(r.Context()),
				)
				w.Header().Set("Content-Type", "application/problem+json")
				w.WriteHeader(http.StatusInternalServerError)
				_ = json.NewEncoder(w).Encode(map[string]any{
					"type":     "https://mariposa.local/problemas/error-interno",
					"title":    "Error interno",
					"status":   http.StatusInternalServerError,
					"detail":   "Ocurrio un error inesperado",
					"instance": r.URL.Path,
				})
			}
		}()
		siguiente.ServeHTTP(w, r)
	})
}
