package middleware

import (
	"context"
	"net/http"

	"github.com/google/uuid"
)

const HeaderIDSolicitud = "X-Request-Id"

type claveContexto struct{}

var claveIDSolicitud = claveContexto{}

func IDSolicitudDesdeContexto(ctx context.Context) string {
	if v, ok := ctx.Value(claveIDSolicitud).(string); ok {
		return v
	}
	return ""
}

func IDSolicitud(siguiente http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		id := r.Header.Get(HeaderIDSolicitud)
		if id == "" {
			id = uuid.NewString()
		}
		w.Header().Set(HeaderIDSolicitud, id)
		ctx := context.WithValue(r.Context(), claveIDSolicitud, id)
		siguiente.ServeHTTP(w, r.WithContext(ctx))
	})
}
