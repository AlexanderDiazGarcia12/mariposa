package main

import (
	"context"
	"log/slog"
	"os"
	"os/signal"
	"syscall"

	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/aplicacion"
	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/configuracion"
	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/infraestructura/cliente_biblioteca"
	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/infraestructura/persistencia"
	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/infraestructura/web"
)

func main() {
	logger := slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{Level: slog.LevelInfo}))
	slog.SetDefault(logger)

	slog.Info("iniciando servicio de prestamos")

	cfg, err := configuracion.Cargar()
	if err != nil {
		slog.Error("error cargando configuracion", "error", err.Error())
		os.Exit(1)
	}

	ctxConexion, cancelConexion := context.WithCancel(context.Background())
	defer cancelConexion()

	pool, err := persistencia.NuevaConexion(ctxConexion, cfg.BaseDeDatos)
	if err != nil {
		slog.Error("error conectando a postgres", "error", err.Error())
		os.Exit(1)
	}
	defer pool.Close()
	slog.Info("conexion a postgres establecida")

	if err := persistencia.AplicarMigraciones(cfg.BaseDeDatos); err != nil {
		slog.Error("error aplicando migraciones", "error", err.Error())
		os.Exit(1)
	}
	slog.Info("migraciones aplicadas")

	repo := persistencia.NuevoRepositorioPrestamoPgx(pool)
	cliente := cliente_biblioteca.NuevoClienteHTTP(
		cfg.URLServicioBiblioteca,
		cfg.SecretoServicioInterno,
		cfg.TimeoutClienteHTTP,
	)

	registrar := aplicacion.NuevoRegistrarPrestamo(repo, cliente)
	devolver := aplicacion.NuevoDevolverPrestamo(repo)
	consultar := aplicacion.NuevoConsultarPrestamo(repo)
	listarPorU := aplicacion.NuevoListarPrestamosPorUsuario(repo)
	listar := aplicacion.NuevoListarPrestamos(repo)

	srv := web.NuevoServidor(registrar, devolver, consultar, listarPorU, listar)

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	if err := srv.Iniciar(ctx, cfg.PuertoHTTP); err != nil {
		slog.Error("servidor terminado con error", "error", err.Error())
		os.Exit(1)
	}
	slog.Info("servicio detenido limpiamente")
}
