package dominio

import (
	"fmt"
	"time"

	"github.com/google/uuid"
)

type Prestamo struct {
	id                      uuid.UUID
	idUsuario               uuid.UUID
	idLibro                 uuid.UUID
	fechaPrestamo           time.Time
	fechaDevolucionEstimada time.Time
	fechaDevolucionReal     *time.Time
	estado                  EstadoPrestamo
	fechaCreacion           time.Time
	fechaActualizacion      time.Time
}

func NuevoPrestamo(
	idUsuario uuid.UUID,
	idLibro uuid.UUID,
	fechaPrestamo time.Time,
	fechaDevolucionEstimada time.Time,
) (Prestamo, error) {
	if idUsuario == uuid.Nil || idLibro == uuid.Nil {
		return Prestamo{}, ErrIDsInvalidos
	}
	if !fechaDevolucionEstimada.After(fechaPrestamo) {
		return Prestamo{}, ErrFechaDevolucionInvalida
	}

	ahora := time.Now().UTC()
	return Prestamo{
		id:                      uuid.New(),
		idUsuario:               idUsuario,
		idLibro:                 idLibro,
		fechaPrestamo:           truncarADia(fechaPrestamo),
		fechaDevolucionEstimada: truncarADia(fechaDevolucionEstimada),
		fechaDevolucionReal:     nil,
		estado:                  EstadoActivo,
		fechaCreacion:           ahora,
		fechaActualizacion:      ahora,
	}, nil
}

func ReconstruirPrestamo(
	id uuid.UUID,
	idUsuario uuid.UUID,
	idLibro uuid.UUID,
	fechaPrestamo time.Time,
	fechaDevolucionEstimada time.Time,
	fechaDevolucionReal *time.Time,
	estado EstadoPrestamo,
	fechaCreacion time.Time,
	fechaActualizacion time.Time,
) Prestamo {
	return Prestamo{
		id:                      id,
		idUsuario:               idUsuario,
		idLibro:                 idLibro,
		fechaPrestamo:           fechaPrestamo,
		fechaDevolucionEstimada: fechaDevolucionEstimada,
		fechaDevolucionReal:     fechaDevolucionReal,
		estado:                  estado,
		fechaCreacion:           fechaCreacion,
		fechaActualizacion:      fechaActualizacion,
	}
}

func (p Prestamo) Devolver(ahora time.Time) (Prestamo, error) {
	if p.estado == EstadoDevuelto {
		return Prestamo{}, fmt.Errorf("%w: id=%s", ErrPrestamoYaDevuelto, p.id)
	}
	ahoraUTC := ahora.UTC()
	fecha := truncarADia(ahoraUTC)
	nuevo := p
	nuevo.estado = EstadoDevuelto
	nuevo.fechaDevolucionReal = &fecha
	nuevo.fechaActualizacion = ahoraUTC
	return nuevo, nil
}

func (p Prestamo) EstaAtrasado(ahora time.Time) bool {
	if p.estado != EstadoActivo {
		return false
	}
	return ahora.After(p.fechaDevolucionEstimada)
}

func (p Prestamo) ID() uuid.UUID            { return p.id }
func (p Prestamo) IDUsuario() uuid.UUID     { return p.idUsuario }
func (p Prestamo) IDLibro() uuid.UUID       { return p.idLibro }
func (p Prestamo) FechaPrestamo() time.Time { return p.fechaPrestamo }
func (p Prestamo) FechaDevolucionEstimada() time.Time {
	return p.fechaDevolucionEstimada
}
func (p Prestamo) FechaDevolucionReal() *time.Time { return p.fechaDevolucionReal }
func (p Prestamo) Estado() EstadoPrestamo          { return p.estado }
func (p Prestamo) FechaCreacion() time.Time        { return p.fechaCreacion }
func (p Prestamo) FechaActualizacion() time.Time   { return p.fechaActualizacion }

func truncarADia(t time.Time) time.Time {
	t = t.UTC()
	return time.Date(t.Year(), t.Month(), t.Day(), 0, 0, 0, 0, time.UTC)
}
