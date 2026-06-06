package dominio

import (
	"errors"
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestNuevoPrestamo_Exitoso(t *testing.T) {
	idUsuario := uuid.New()
	idLibro := uuid.New()
	fp := time.Date(2026, 6, 1, 0, 0, 0, 0, time.UTC)
	fde := time.Date(2026, 6, 15, 0, 0, 0, 0, time.UTC)

	p, err := NuevoPrestamo(idUsuario, idLibro, fp, fde)
	require.NoError(t, err)

	assert.NotEqual(t, uuid.Nil, p.ID())
	assert.Equal(t, idUsuario, p.IDUsuario())
	assert.Equal(t, idLibro, p.IDLibro())
	assert.Equal(t, EstadoActivo, p.Estado())
	assert.Nil(t, p.FechaDevolucionReal())
	assert.True(t, p.FechaCreacion().Equal(p.FechaActualizacion()))
	assert.Equal(t, fp, p.FechaPrestamo())
	assert.Equal(t, fde, p.FechaDevolucionEstimada())
}

func TestNuevoPrestamo_FechaDevolucionInvalida(t *testing.T) {
	idUsuario := uuid.New()
	idLibro := uuid.New()
	fp := time.Date(2026, 6, 1, 0, 0, 0, 0, time.UTC)
	fde := fp

	_, err := NuevoPrestamo(idUsuario, idLibro, fp, fde)
	require.Error(t, err)
	assert.True(t, errors.Is(err, ErrFechaDevolucionInvalida))
}

func TestNuevoPrestamo_IDsInvalidos(t *testing.T) {
	fp := time.Date(2026, 6, 1, 0, 0, 0, 0, time.UTC)
	fde := fp.Add(24 * time.Hour)

	_, err := NuevoPrestamo(uuid.Nil, uuid.New(), fp, fde)
	require.Error(t, err)
	assert.True(t, errors.Is(err, ErrIDsInvalidos))

	_, err = NuevoPrestamo(uuid.New(), uuid.Nil, fp, fde)
	require.Error(t, err)
	assert.True(t, errors.Is(err, ErrIDsInvalidos))
}

func TestDevolver_Exitoso(t *testing.T) {
	p, err := NuevoPrestamo(uuid.New(), uuid.New(),
		time.Date(2026, 6, 1, 0, 0, 0, 0, time.UTC),
		time.Date(2026, 6, 15, 0, 0, 0, 0, time.UTC))
	require.NoError(t, err)

	ahora := time.Date(2026, 6, 10, 12, 30, 0, 0, time.UTC)
	devuelto, err := p.Devolver(ahora)
	require.NoError(t, err)

	assert.Equal(t, EstadoDevuelto, devuelto.Estado())
	require.NotNil(t, devuelto.FechaDevolucionReal())
	assert.Equal(t, time.Date(2026, 6, 10, 0, 0, 0, 0, time.UTC), *devuelto.FechaDevolucionReal())
	assert.True(t, ahora.Equal(devuelto.FechaActualizacion()))

	assert.Equal(t, EstadoActivo, p.Estado())
	assert.Nil(t, p.FechaDevolucionReal())
}

func TestDevolver_YaDevuelto(t *testing.T) {
	p, err := NuevoPrestamo(uuid.New(), uuid.New(),
		time.Date(2026, 6, 1, 0, 0, 0, 0, time.UTC),
		time.Date(2026, 6, 15, 0, 0, 0, 0, time.UTC))
	require.NoError(t, err)

	devuelto, err := p.Devolver(time.Date(2026, 6, 10, 0, 0, 0, 0, time.UTC))
	require.NoError(t, err)

	_, err = devuelto.Devolver(time.Date(2026, 6, 11, 0, 0, 0, 0, time.UTC))
	require.Error(t, err)
	assert.True(t, errors.Is(err, ErrPrestamoYaDevuelto))
}

func TestEstaAtrasado_VerdaderoSiActivoYPasada(t *testing.T) {
	p, err := NuevoPrestamo(uuid.New(), uuid.New(),
		time.Date(2026, 6, 1, 0, 0, 0, 0, time.UTC),
		time.Date(2026, 6, 5, 0, 0, 0, 0, time.UTC))
	require.NoError(t, err)

	ahora := time.Date(2026, 6, 10, 12, 0, 0, 0, time.UTC)
	assert.True(t, p.EstaAtrasado(ahora))
}

func TestEstaAtrasado_FalsoSiDentroDelPlazo(t *testing.T) {
	p, err := NuevoPrestamo(uuid.New(), uuid.New(),
		time.Date(2026, 6, 1, 0, 0, 0, 0, time.UTC),
		time.Date(2026, 6, 30, 0, 0, 0, 0, time.UTC))
	require.NoError(t, err)

	ahora := time.Date(2026, 6, 10, 12, 0, 0, 0, time.UTC)
	assert.False(t, p.EstaAtrasado(ahora))
}

func TestEstaAtrasado_FalsoSiDevuelto(t *testing.T) {
	p, err := NuevoPrestamo(uuid.New(), uuid.New(),
		time.Date(2026, 6, 1, 0, 0, 0, 0, time.UTC),
		time.Date(2026, 6, 5, 0, 0, 0, 0, time.UTC))
	require.NoError(t, err)

	devuelto, err := p.Devolver(time.Date(2026, 6, 3, 0, 0, 0, 0, time.UTC))
	require.NoError(t, err)

	ahora := time.Date(2026, 6, 20, 0, 0, 0, 0, time.UTC)
	assert.False(t, devuelto.EstaAtrasado(ahora))
}

func TestEstadoPrestamo_Valido(t *testing.T) {
	assert.True(t, EstadoActivo.Valido())
	assert.True(t, EstadoDevuelto.Valido())
	assert.False(t, EstadoPrestamo("OTRO").Valido())
}

func TestReconstruirPrestamo_ConservaTodosLosCampos(t *testing.T) {
	id := uuid.New()
	idU := uuid.New()
	idL := uuid.New()
	fp := time.Date(2026, 1, 1, 0, 0, 0, 0, time.UTC)
	fde := time.Date(2026, 1, 15, 0, 0, 0, 0, time.UTC)
	fdr := time.Date(2026, 1, 14, 0, 0, 0, 0, time.UTC)
	fc := time.Date(2026, 1, 1, 10, 0, 0, 0, time.UTC)
	fa := time.Date(2026, 1, 14, 10, 0, 0, 0, time.UTC)

	p := ReconstruirPrestamo(id, idU, idL, fp, fde, &fdr, EstadoDevuelto, fc, fa)
	assert.Equal(t, id, p.ID())
	assert.Equal(t, idU, p.IDUsuario())
	assert.Equal(t, idL, p.IDLibro())
	assert.Equal(t, fp, p.FechaPrestamo())
	assert.Equal(t, fde, p.FechaDevolucionEstimada())
	require.NotNil(t, p.FechaDevolucionReal())
	assert.Equal(t, fdr, *p.FechaDevolucionReal())
	assert.Equal(t, EstadoDevuelto, p.Estado())
	assert.Equal(t, fc, p.FechaCreacion())
	assert.Equal(t, fa, p.FechaActualizacion())
}
