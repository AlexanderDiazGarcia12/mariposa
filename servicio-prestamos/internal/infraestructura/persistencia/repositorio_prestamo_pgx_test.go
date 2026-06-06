package persistencia

import (
	"context"
	"errors"
	"fmt"
	"os"
	"testing"
	"time"

	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/dominio"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"github.com/testcontainers/testcontainers-go"
	postgrescontenedor "github.com/testcontainers/testcontainers-go/modules/postgres"
	"github.com/testcontainers/testcontainers-go/wait"
)

var (
	dsnPrueba  string
	poolPrueba *pgxpool.Pool
)

func TestMain(m *testing.M) {
	if os.Getenv("OMITIR_TESTS_INTEGRACION") == "1" {
		os.Exit(0)
	}
	ctx := context.Background()

	contenedor, err := postgrescontenedor.Run(ctx,
		"postgres:16-alpine",
		postgrescontenedor.WithDatabase("prestamos_test"),
		postgrescontenedor.WithUsername("prestamos_test"),
		postgrescontenedor.WithPassword("prestamos_test"),
		testcontainers.WithWaitStrategy(
			wait.ForLog("database system is ready to accept connections").
				WithOccurrence(2).
				WithStartupTimeout(60*time.Second),
		),
	)
	if err != nil {
		fmt.Fprintf(os.Stderr, "no se pudo arrancar postgres testcontainer: %v\n", err)
		os.Exit(0)
	}
	defer func() { _ = contenedor.Terminate(ctx) }()

	dsn, err := contenedor.ConnectionString(ctx, "sslmode=disable")
	if err != nil {
		fmt.Fprintf(os.Stderr, "no se pudo obtener dsn: %v\n", err)
		os.Exit(1)
	}
	dsnPrueba = dsn

	if err := AplicarMigraciones(dsn); err != nil {
		fmt.Fprintf(os.Stderr, "fallo migraciones: %v\n", err)
		os.Exit(1)
	}

	pool, err := NuevaConexion(ctx, dsn)
	if err != nil {
		fmt.Fprintf(os.Stderr, "fallo conexion: %v\n", err)
		os.Exit(1)
	}
	poolPrueba = pool
	defer pool.Close()

	codigo := m.Run()
	os.Exit(codigo)
}

func limpiarTabla(t *testing.T) {
	t.Helper()
	_, err := poolPrueba.Exec(context.Background(), "DELETE FROM prestamos")
	require.NoError(t, err)
}

func nuevoPrestamoValido(t *testing.T) dominio.Prestamo {
	t.Helper()
	p, err := dominio.NuevoPrestamo(
		uuid.New(), uuid.New(),
		time.Date(2026, 6, 1, 0, 0, 0, 0, time.UTC),
		time.Date(2026, 6, 15, 0, 0, 0, 0, time.UTC),
	)
	require.NoError(t, err)
	return p
}

func TestRepositorio_GuardarYObtenerPorID(t *testing.T) {
	if poolPrueba == nil {
		t.Skip("postgres testcontainer no disponible")
	}
	limpiarTabla(t)
	repo := NuevoRepositorioPrestamoPgx(poolPrueba)
	p := nuevoPrestamoValido(t)

	require.NoError(t, repo.Guardar(context.Background(), p))

	encontrado, err := repo.ObtenerPorID(context.Background(), p.ID())
	require.NoError(t, err)
	assert.Equal(t, p.ID(), encontrado.ID())
	assert.Equal(t, p.IDUsuario(), encontrado.IDUsuario())
	assert.Equal(t, p.IDLibro(), encontrado.IDLibro())
	assert.Equal(t, p.Estado(), encontrado.Estado())
	assert.True(t, p.FechaPrestamo().Equal(encontrado.FechaPrestamo()))
	assert.True(t, p.FechaDevolucionEstimada().Equal(encontrado.FechaDevolucionEstimada()))
	assert.Nil(t, encontrado.FechaDevolucionReal())
}

func TestRepositorio_ObtenerPorID_NoEncontrado(t *testing.T) {
	if poolPrueba == nil {
		t.Skip("postgres testcontainer no disponible")
	}
	limpiarTabla(t)
	repo := NuevoRepositorioPrestamoPgx(poolPrueba)
	_, err := repo.ObtenerPorID(context.Background(), uuid.New())
	require.Error(t, err)
	assert.True(t, errors.Is(err, dominio.ErrPrestamoNoEncontrado))
}

func TestRepositorio_UpsertActualiza(t *testing.T) {
	if poolPrueba == nil {
		t.Skip("postgres testcontainer no disponible")
	}
	limpiarTabla(t)
	repo := NuevoRepositorioPrestamoPgx(poolPrueba)
	p := nuevoPrestamoValido(t)
	require.NoError(t, repo.Guardar(context.Background(), p))

	devuelto, err := p.Devolver(time.Date(2026, 6, 10, 12, 0, 0, 0, time.UTC))
	require.NoError(t, err)
	require.NoError(t, repo.Guardar(context.Background(), devuelto))

	encontrado, err := repo.ObtenerPorID(context.Background(), p.ID())
	require.NoError(t, err)
	assert.Equal(t, dominio.EstadoDevuelto, encontrado.Estado())
	require.NotNil(t, encontrado.FechaDevolucionReal())
}

func TestRepositorio_ListarPorUsuarioPaginado(t *testing.T) {
	if poolPrueba == nil {
		t.Skip("postgres testcontainer no disponible")
	}
	limpiarTabla(t)
	repo := NuevoRepositorioPrestamoPgx(poolPrueba)
	idUsuario := uuid.New()

	for i := 0; i < 5; i++ {
		p, err := dominio.NuevoPrestamo(idUsuario, uuid.New(),
			time.Date(2026, 6, 1+i, 0, 0, 0, 0, time.UTC),
			time.Date(2026, 6, 20+i, 0, 0, 0, 0, time.UTC))
		require.NoError(t, err)
		require.NoError(t, repo.Guardar(context.Background(), p))
		time.Sleep(2 * time.Millisecond)
	}

	pag, err := repo.ListarPorUsuario(context.Background(), idUsuario, 0, 2)
	require.NoError(t, err)
	assert.Equal(t, int64(5), pag.TotalElementos)
	assert.Equal(t, 3, pag.TotalPaginas)
	assert.Len(t, pag.Elementos, 2)

	pag2, err := repo.ListarPorUsuario(context.Background(), idUsuario, 2, 2)
	require.NoError(t, err)
	assert.Len(t, pag2.Elementos, 1)
}

func TestRepositorio_Listar(t *testing.T) {
	if poolPrueba == nil {
		t.Skip("postgres testcontainer no disponible")
	}
	limpiarTabla(t)
	repo := NuevoRepositorioPrestamoPgx(poolPrueba)

	for i := 0; i < 3; i++ {
		require.NoError(t, repo.Guardar(context.Background(), nuevoPrestamoValido(t)))
	}

	pag, err := repo.Listar(context.Background(), 0, 10)
	require.NoError(t, err)
	assert.Equal(t, int64(3), pag.TotalElementos)
	assert.Equal(t, 1, pag.TotalPaginas)
	assert.Len(t, pag.Elementos, 3)
}
