package aplicacion

import (
	"context"

	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/dominio"
	"github.com/google/uuid"
)

type RepositorioPrestamo interface {
	Guardar(ctx context.Context, p dominio.Prestamo) error
	ObtenerPorID(ctx context.Context, id uuid.UUID) (dominio.Prestamo, error)
	ListarPorUsuario(ctx context.Context, idUsuario uuid.UUID, pagina, tamano int) (Pagina[dominio.Prestamo], error)
	Listar(ctx context.Context, pagina, tamano int) (Pagina[dominio.Prestamo], error)
}

type ClienteBiblioteca interface {
	ValidarLibroDisponible(ctx context.Context, idLibro uuid.UUID) error
}

type Pagina[T any] struct {
	Elementos      []T
	PaginaActual   int
	TamanoPagina   int
	TotalElementos int64
	TotalPaginas   int
}
