package aplicacion

import (
	"context"

	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/dominio"
	"github.com/google/uuid"
)

type ConsultarPrestamo struct {
	repositorio RepositorioPrestamo
}

func NuevoConsultarPrestamo(repositorio RepositorioPrestamo) *ConsultarPrestamo {
	return &ConsultarPrestamo{repositorio: repositorio}
}

func (uc *ConsultarPrestamo) Ejecutar(ctx context.Context, id uuid.UUID) (dominio.Prestamo, error) {
	return uc.repositorio.ObtenerPorID(ctx, id)
}
