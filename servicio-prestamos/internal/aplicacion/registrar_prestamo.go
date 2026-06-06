package aplicacion

import (
	"context"
	"fmt"
	"time"

	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/dominio"
	"github.com/google/uuid"
)

type RegistrarPrestamoComando struct {
	IDUsuario               uuid.UUID
	IDLibro                 uuid.UUID
	FechaPrestamo           time.Time
	FechaDevolucionEstimada time.Time
}

type RegistrarPrestamo struct {
	repositorio RepositorioPrestamo
	cliente     ClienteBiblioteca
}

func NuevoRegistrarPrestamo(repositorio RepositorioPrestamo, cliente ClienteBiblioteca) *RegistrarPrestamo {
	return &RegistrarPrestamo{repositorio: repositorio, cliente: cliente}
}

func (uc *RegistrarPrestamo) Ejecutar(ctx context.Context, cmd RegistrarPrestamoComando) (dominio.Prestamo, error) {
	if err := uc.cliente.ValidarLibroDisponible(ctx, cmd.IDLibro); err != nil {
		return dominio.Prestamo{}, err
	}

	prestamo, err := dominio.NuevoPrestamo(cmd.IDUsuario, cmd.IDLibro, cmd.FechaPrestamo, cmd.FechaDevolucionEstimada)
	if err != nil {
		return dominio.Prestamo{}, err
	}

	if err := uc.repositorio.Guardar(ctx, prestamo); err != nil {
		return dominio.Prestamo{}, fmt.Errorf("guardando prestamo: %w", err)
	}
	return prestamo, nil
}
