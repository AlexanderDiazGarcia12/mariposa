package middleware

import (
	"context"
	"net/http"

	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/observabilidad"
	"github.com/google/uuid"
)

const HeaderIDSolicitud = observabilidad.HeaderIDSolicitud

func IDSolicitudDesdeContexto(ctx context.Context) string {
	return observabilidad.IDSolicitudDesdeContexto(ctx)
}

func IDSolicitud(siguiente http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		id := r.Header.Get(HeaderIDSolicitud)
		if id == "" {
			id = uuid.NewString()
		}
		w.Header().Set(HeaderIDSolicitud, id)
		ctx := observabilidad.ContextoConIDSolicitud(r.Context(), id)
		siguiente.ServeHTTP(w, r.WithContext(ctx))
	})
}
