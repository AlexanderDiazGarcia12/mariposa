package aplicacion

import (
	"context"
	"fmt"
	"time"

	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/dominio"
	"github.com/google/uuid"
)

type DevolverPrestamoComando struct {
	ID uuid.UUID
}

type DevolverPrestamo struct {
	repositorio RepositorioPrestamo
	reloj       func() time.Time
}

func NuevoDevolverPrestamo(repositorio RepositorioPrestamo) *DevolverPrestamo {
	return &DevolverPrestamo{repositorio: repositorio, reloj: func() time.Time { return time.Now().UTC() }}
}

func (uc *DevolverPrestamo) ConReloj(reloj func() time.Time) *DevolverPrestamo {
	uc.reloj = reloj
	return uc
}

func (uc *DevolverPrestamo) Ejecutar(ctx context.Context, cmd DevolverPrestamoComando) (dominio.Prestamo, error) {
	prestamo, err := uc.repositorio.ObtenerPorID(ctx, cmd.ID)
	if err != nil {
		return dominio.Prestamo{}, err
	}
	devuelto, err := prestamo.Devolver(uc.reloj())
	if err != nil {
		return dominio.Prestamo{}, err
	}
	if err := uc.repositorio.Guardar(ctx, devuelto); err != nil {
		return dominio.Prestamo{}, fmt.Errorf("guardando devolucion: %w", err)
	}
	return devuelto, nil
}
