package middleware

import (
	"encoding/json"
	"net/http"
)

func AuthInterna(secreto string) func(http.Handler) http.Handler {
	const nombreHeader = "X-Servicio-Interno"
	return func(siguiente http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			if secreto == "" {
				siguiente.ServeHTTP(w, r)
				return
			}
			if r.Header.Get(nombreHeader) != secreto {
				w.Header().Set("Content-Type", "application/problem+json")
				w.WriteHeader(http.StatusUnauthorized)
				_ = json.NewEncoder(w).Encode(map[string]any{
					"type":     "https://mariposa.local/problemas/no-autorizado",
					"title":    "No autorizado",
					"status":   http.StatusUnauthorized,
					"detail":   "Header " + nombreHeader + " invalido o ausente",
					"instance": r.URL.Path,
				})
				return
			}
			siguiente.ServeHTTP(w, r)
		})
	}
}
