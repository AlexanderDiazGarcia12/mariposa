package aplicacion

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/dominio"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

type stubRepositorio struct {
	guardados        []dominio.Prestamo
	errGuardar       error
	porID            map[uuid.UUID]dominio.Prestamo
	errObtenerPorID  error
	listarPorUsuario Pagina[dominio.Prestamo]
	errListar        error
}

func nuevoStubRepositorio() *stubRepositorio {
	return &stubRepositorio{porID: map[uuid.UUID]dominio.Prestamo{}}
}

func (r *stubRepositorio) Guardar(_ context.Context, p dominio.Prestamo) error {
	if r.errGuardar != nil {
		return r.errGuardar
	}
	r.guardados = append(r.guardados, p)
	r.porID[p.ID()] = p
	return nil
}

func (r *stubRepositorio) ObtenerPorID(_ context.Context, id uuid.UUID) (dominio.Prestamo, error) {
	if r.errObtenerPorID != nil {
		return dominio.Prestamo{}, r.errObtenerPorID
	}
	p, ok := r.porID[id]
	if !ok {
		return dominio.Prestamo{}, dominio.ErrPrestamoNoEncontrado
	}
	return p, nil
}

func (r *stubRepositorio) ListarPorUsuario(_ context.Context, _ uuid.UUID, _, _ int) (Pagina[dominio.Prestamo], error) {
	if r.errListar != nil {
		return Pagina[dominio.Prestamo]{}, r.errListar
	}
	return r.listarPorUsuario, nil
}

func (r *stubRepositorio) Listar(_ context.Context, _, _ int) (Pagina[dominio.Prestamo], error) {
	if r.errListar != nil {
		return Pagina[dominio.Prestamo]{}, r.errListar
	}
	return r.listarPorUsuario, nil
}

type stubCliente struct {
	err error
}

func (c *stubCliente) ValidarLibroDisponible(_ context.Context, _ uuid.UUID) error {
	return c.err
}

func cmdValido() RegistrarPrestamoComando {
	return RegistrarPrestamoComando{
		IDUsuario:               uuid.New(),
		IDLibro:                 uuid.New(),
		FechaPrestamo:           time.Date(2026, 6, 1, 0, 0, 0, 0, time.UTC),
		FechaDevolucionEstimada: time.Date(2026, 6, 15, 0, 0, 0, 0, time.UTC),
	}
}

func TestRegistrarPrestamo_Exitoso(t *testing.T) {
	repo := nuevoStubRepositorio()
	cli := &stubCliente{}
	uc := NuevoRegistrarPrestamo(repo, cli)

	p, err := uc.Ejecutar(context.Background(), cmdValido())
	require.NoError(t, err)
	assert.Len(t, repo.guardados, 1)
	assert.Equal(t, dominio.EstadoActivo, p.Estado())
}

func TestRegistrarPrestamo_LibroNoExiste(t *testing.T) {
	repo := nuevoStubRepositorio()
	cli := &stubCliente{err: dominio.ErrLibroNoEncontrado}
	uc := NuevoRegistrarPrestamo(repo, cli)

	_, err := uc.Ejecutar(context.Background(), cmdValido())
	require.Error(t, err)
	assert.True(t, errors.Is(err, dominio.ErrLibroNoEncontrado))
	assert.Empty(t, repo.guardados)
}

func TestRegistrarPrestamo_SinCopias(t *testing.T) {
	repo := nuevoStubRepositorio()
	cli := &stubCliente{err: dominio.ErrSinCopiasDisponibles}
	uc := NuevoRegistrarPrestamo(repo, cli)

	_, err := uc.Ejecutar(context.Background(), cmdValido())
	require.Error(t, err)
	assert.True(t, errors.Is(err, dominio.ErrSinCopiasDisponibles))
	assert.Empty(t, repo.guardados)
}

func TestRegistrarPrestamo_ServicioBibliotecaNoDisponible(t *testing.T) {
	repo := nuevoStubRepositorio()
	cli := &stubCliente{err: dominio.ErrServicioBibliotecaNoDisponible}
	uc := NuevoRegistrarPrestamo(repo, cli)

	_, err := uc.Ejecutar(context.Background(), cmdValido())
	require.Error(t, err)
	assert.True(t, errors.Is(err, dominio.ErrServicioBibliotecaNoDisponible))
}

func TestRegistrarPrestamo_FechasInvalidas(t *testing.T) {
	repo := nuevoStubRepositorio()
	cli := &stubCliente{}
	uc := NuevoRegistrarPrestamo(repo, cli)

	cmd := cmdValido()
	cmd.FechaDevolucionEstimada = cmd.FechaPrestamo
	_, err := uc.Ejecutar(context.Background(), cmd)
	require.Error(t, err)
	assert.True(t, errors.Is(err, dominio.ErrFechaDevolucionInvalida))
}

func TestDevolverPrestamo_Exitoso(t *testing.T) {
	repo := nuevoStubRepositorio()
	p, err := dominio.NuevoPrestamo(uuid.New(), uuid.New(),
		time.Date(2026, 6, 1, 0, 0, 0, 0, time.UTC),
		time.Date(2026, 6, 15, 0, 0, 0, 0, time.UTC))
	require.NoError(t, err)
	require.NoError(t, repo.Guardar(context.Background(), p))

	uc := NuevoDevolverPrestamo(repo).ConReloj(func() time.Time {
		return time.Date(2026, 6, 10, 0, 0, 0, 0, time.UTC)
	})

	devuelto, err := uc.Ejecutar(context.Background(), DevolverPrestamoComando{ID: p.ID()})
	require.NoError(t, err)
	assert.Equal(t, dominio.EstadoDevuelto, devuelto.Estado())
}

func TestDevolverPrestamo_NoEncontrado(t *testing.T) {
	repo := nuevoStubRepositorio()
	uc := NuevoDevolverPrestamo(repo)

	_, err := uc.Ejecutar(context.Background(), DevolverPrestamoComando{ID: uuid.New()})
	require.Error(t, err)
	assert.True(t, errors.Is(err, dominio.ErrPrestamoNoEncontrado))
}

func TestDevolverPrestamo_YaDevuelto(t *testing.T) {
	repo := nuevoStubRepositorio()
	p, _ := dominio.NuevoPrestamo(uuid.New(), uuid.New(),
		time.Date(2026, 6, 1, 0, 0, 0, 0, time.UTC),
		time.Date(2026, 6, 15, 0, 0, 0, 0, time.UTC))
	devuelto, _ := p.Devolver(time.Date(2026, 6, 5, 0, 0, 0, 0, time.UTC))
	require.NoError(t, repo.Guardar(context.Background(), devuelto))

	uc := NuevoDevolverPrestamo(repo)
	_, err := uc.Ejecutar(context.Background(), DevolverPrestamoComando{ID: devuelto.ID()})
	require.Error(t, err)
	assert.True(t, errors.Is(err, dominio.ErrPrestamoYaDevuelto))
}

func TestConsultarPrestamo_Exitoso(t *testing.T) {
	repo := nuevoStubRepositorio()
	p, _ := dominio.NuevoPrestamo(uuid.New(), uuid.New(),
		time.Date(2026, 6, 1, 0, 0, 0, 0, time.UTC),
		time.Date(2026, 6, 15, 0, 0, 0, 0, time.UTC))
	require.NoError(t, repo.Guardar(context.Background(), p))

	uc := NuevoConsultarPrestamo(repo)
	encontrado, err := uc.Ejecutar(context.Background(), p.ID())
	require.NoError(t, err)
	assert.Equal(t, p.ID(), encontrado.ID())
}

func TestListar_NormalizaPaginacion(t *testing.T) {
	repo := nuevoStubRepositorio()
	uc := NuevoListarPrestamos(repo)
	_, err := uc.Ejecutar(context.Background(), -1, 0)
	require.NoError(t, err)
}
