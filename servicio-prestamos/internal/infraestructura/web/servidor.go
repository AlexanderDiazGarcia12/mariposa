package web

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"log/slog"
	"net/http"
	"time"

	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/aplicacion"
	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/infraestructura/web/controladores"
	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/infraestructura/web/middleware"
)

type Servidor struct {
	mux *http.ServeMux
}

func NuevoServidor(
	registrar *aplicacion.RegistrarPrestamo,
	devolver *aplicacion.DevolverPrestamo,
	consultar *aplicacion.ConsultarPrestamo,
	listarPorU *aplicacion.ListarPrestamosPorUsuario,
	listar *aplicacion.ListarPrestamos,
) *Servidor {
	controlador := controladores.Nuevo(registrar, devolver, consultar, listarPorU, listar, ManejarError)

	mux := http.NewServeMux()
	mux.HandleFunc("POST /api/v1/prestamos", controlador.Registrar)
	mux.HandleFunc("GET /api/v1/prestamos", controlador.Listar)
	mux.HandleFunc("GET /api/v1/prestamos/{id}", controlador.Consultar)
	mux.HandleFunc("POST /api/v1/prestamos/{id}/devolver", controlador.Devolver)
	mux.HandleFunc("GET /api/v1/prestamos/usuario/{idUsuario}", controlador.ListarPorUsuario)
	mux.HandleFunc("GET /salud", manejadorSalud)

	return &Servidor{mux: mux}
}

func (s *Servidor) Handler() http.Handler {
	return componerMiddleware(
		s.mux,
		middleware.IDSolicitud,
		middleware.Logging,
		middleware.Recuperacion,
	)
}

func (s *Servidor) Iniciar(ctx context.Context, puerto int) error {
	httpSrv := &http.Server{
		Addr:              fmt.Sprintf(":%d", puerto),
		Handler:           s.Handler(),
		ReadHeaderTimeout: 10 * time.Second,
		ReadTimeout:       30 * time.Second,
		WriteTimeout:      30 * time.Second,
		IdleTimeout:       120 * time.Second,
	}

	errores := make(chan error, 1)
	go func() {
		slog.Info("servidor HTTP escuchando", "puerto", puerto)
		if err := httpSrv.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			errores <- err
		}
		close(errores)
	}()

	select {
	case <-ctx.Done():
		slog.Info("recibida senal de parada, iniciando graceful shutdown")
		ctxApagado, cancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer cancel()
		if err := httpSrv.Shutdown(ctxApagado); err != nil {
			return fmt.Errorf("shutdown: %w", err)
		}
		slog.Info("servidor HTTP detenido")
		return nil
	case err, ok := <-errores:
		if ok && err != nil {
			return fmt.Errorf("listen: %w", err)
		}
		return nil
	}
}

func componerMiddleware(h http.Handler, mw ...func(http.Handler) http.Handler) http.Handler {
	for i := len(mw) - 1; i >= 0; i-- {
		h = mw[i](h)
	}
	return h
}

func manejadorSalud(w http.ResponseWriter, _ *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	_ = json.NewEncoder(w).Encode(map[string]string{"estado": "OK"})
}
