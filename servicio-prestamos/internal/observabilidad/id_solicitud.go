package observabilidad

import "context"

const HeaderIDSolicitud = "X-Request-Id"

type claveContexto struct{}

var claveIDSolicitud = claveContexto{}

func ContextoConIDSolicitud(ctx context.Context, id string) context.Context {
	return context.WithValue(ctx, claveIDSolicitud, id)
}

func IDSolicitudDesdeContexto(ctx context.Context) string {
	if v, ok := ctx.Value(claveIDSolicitud).(string); ok {
		return v
	}
	return ""
}
