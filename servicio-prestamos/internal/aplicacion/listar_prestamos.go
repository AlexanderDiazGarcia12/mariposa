package aplicacion

import (
	"context"

	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/dominio"
	"github.com/google/uuid"
)

type ListarPrestamos struct {
	repositorio RepositorioPrestamo
}

func NuevoListarPrestamos(repositorio RepositorioPrestamo) *ListarPrestamos {
	return &ListarPrestamos{repositorio: repositorio}
}

func (uc *ListarPrestamos) Ejecutar(ctx context.Context, pagina, tamano int) (Pagina[dominio.Prestamo], error) {
	pagina, tamano = normalizarPaginacion(pagina, tamano)
	return uc.repositorio.Listar(ctx, pagina, tamano)
}

type ListarPrestamosPorUsuario struct {
	repositorio RepositorioPrestamo
}

func NuevoListarPrestamosPorUsuario(repositorio RepositorioPrestamo) *ListarPrestamosPorUsuario {
	return &ListarPrestamosPorUsuario{repositorio: repositorio}
}

func (uc *ListarPrestamosPorUsuario) Ejecutar(ctx context.Context, idUsuario uuid.UUID, pagina, tamano int) (Pagina[dominio.Prestamo], error) {
	pagina, tamano = normalizarPaginacion(pagina, tamano)
	return uc.repositorio.ListarPorUsuario(ctx, idUsuario, pagina, tamano)
}

func normalizarPaginacion(pagina, tamano int) (int, int) {
	if pagina < 0 {
		pagina = 0
	}
	if tamano <= 0 {
		tamano = 20
	}
	if tamano > 100 {
		tamano = 100
	}
	return pagina, tamano
}
