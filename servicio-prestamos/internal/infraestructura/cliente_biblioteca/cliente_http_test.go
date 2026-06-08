package cliente_biblioteca

import (
	"context"
	"errors"
	"fmt"
	"net/http"
	"net/http/httptest"
	"sync/atomic"
	"testing"
	"time"

	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/dominio"
	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/observabilidad"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

const secretoPrueba = "secreto-de-prueba"

func TestValidarLibroDisponible_200_ConCopias(t *testing.T) {
	idLibro := uuid.New()
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		assert.Equal(t, "/api/v1/internal/libros/"+idLibro.String(), r.URL.Path)
		assert.Equal(t, secretoPrueba, r.Header.Get(nombreHeaderInterno))
		w.Header().Set("Content-Type", "application/json")
		fmt.Fprintf(w, `{"id":%q,"titulo":"T","copiasTotales":3,"copiasDisponibles":2}`, idLibro)
	}))
	defer srv.Close()

	cli := NuevoClienteHTTP(srv.URL, secretoPrueba, 2*time.Second).SinReintentos()
	err := cli.ValidarLibroDisponible(context.Background(), idLibro)
	require.NoError(t, err)
}

func TestValidarLibroDisponible_200_SinCopias(t *testing.T) {
	idLibro := uuid.New()
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		fmt.Fprintf(w, `{"id":%q,"copiasTotales":3,"copiasDisponibles":0}`, idLibro)
	}))
	defer srv.Close()

	cli := NuevoClienteHTTP(srv.URL, secretoPrueba, 2*time.Second).SinReintentos()
	err := cli.ValidarLibroDisponible(context.Background(), idLibro)
	require.Error(t, err)
	assert.True(t, errors.Is(err, dominio.ErrSinCopiasDisponibles))
}

func TestValidarLibroDisponible_404(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		http.NotFound(w, r)
	}))
	defer srv.Close()

	cli := NuevoClienteHTTP(srv.URL, secretoPrueba, 2*time.Second).SinReintentos()
	err := cli.ValidarLibroDisponible(context.Background(), uuid.New())
	require.Error(t, err)
	assert.True(t, errors.Is(err, dominio.ErrLibroNoEncontrado))
}

func TestValidarLibroDisponible_500(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		http.Error(w, "boom", http.StatusInternalServerError)
	}))
	defer srv.Close()

	cli := NuevoClienteHTTP(srv.URL, secretoPrueba, 2*time.Second).SinReintentos()
	err := cli.ValidarLibroDisponible(context.Background(), uuid.New())
	require.Error(t, err)
	assert.True(t, errors.Is(err, dominio.ErrServicioBibliotecaNoDisponible))
}

func TestValidarLibroDisponible_Timeout(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		time.Sleep(500 * time.Millisecond)
		w.WriteHeader(http.StatusOK)
	}))
	defer srv.Close()

	cli := NuevoClienteHTTP(srv.URL, secretoPrueba, 50*time.Millisecond).SinReintentos()
	err := cli.ValidarLibroDisponible(context.Background(), uuid.New())
	require.Error(t, err)
	assert.True(t, errors.Is(err, dominio.ErrServicioBibliotecaNoDisponible))
}

func TestValidarLibroDisponible_ReintentaEn5xx(t *testing.T) {
	var llamadas atomic.Int32
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		n := llamadas.Add(1)
		if n < 3 {
			http.Error(w, "transitorio", http.StatusServiceUnavailable)
			return
		}
		w.Header().Set("Content-Type", "application/json")
		fmt.Fprintf(w, `{"copiasDisponibles":1}`)
	}))
	defer srv.Close()

	cli := NuevoClienteHTTP(srv.URL, secretoPrueba, 2*time.Second)
	err := cli.ValidarLibroDisponible(context.Background(), uuid.New())
	require.NoError(t, err)
	assert.Equal(t, int32(3), llamadas.Load())
}

func TestValidarLibroDisponible_NoReintentaEn404(t *testing.T) {
	var llamadas atomic.Int32
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		llamadas.Add(1)
		http.NotFound(w, r)
	}))
	defer srv.Close()

	cli := NuevoClienteHTTP(srv.URL, secretoPrueba, 2*time.Second)
	err := cli.ValidarLibroDisponible(context.Background(), uuid.New())
	require.Error(t, err)
	assert.True(t, errors.Is(err, dominio.ErrLibroNoEncontrado))
	assert.Equal(t, int32(1), llamadas.Load())
}

func TestValidarLibroDisponible_403TratadoComoNoDisponible(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		http.Error(w, "forbidden", http.StatusForbidden)
	}))
	defer srv.Close()

	cli := NuevoClienteHTTP(srv.URL, secretoPrueba, 2*time.Second).SinReintentos()
	err := cli.ValidarLibroDisponible(context.Background(), uuid.New())
	require.Error(t, err)
	assert.True(t, errors.Is(err, dominio.ErrServicioBibliotecaNoDisponible))
}

func TestValidarLibroDisponible_PropagaXRequestIdDesdeContexto(t *testing.T) {
	idLibro := uuid.New()
	idEsperado := "trace-distribuido-77"
	var idRecibido string
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		idRecibido = r.Header.Get(observabilidad.HeaderIDSolicitud)
		w.Header().Set("Content-Type", "application/json")
		fmt.Fprintf(w, `{"id":%q,"titulo":"T","copiasTotales":1,"copiasDisponibles":1}`, idLibro)
	}))
	defer srv.Close()

	cli := NuevoClienteHTTP(srv.URL, secretoPrueba, 2*time.Second).SinReintentos()
	ctx := observabilidad.ContextoConIDSolicitud(context.Background(), idEsperado)

	err := cli.ValidarLibroDisponible(ctx, idLibro)
	require.NoError(t, err)
	assert.Equal(t, idEsperado, idRecibido, "el cliente debe propagar X-Request-Id desde el contexto")
}

func TestValidarLibroDisponible_NoEnviaXRequestIdSinContexto(t *testing.T) {
	idLibro := uuid.New()
	var idRecibido string
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		idRecibido = r.Header.Get(observabilidad.HeaderIDSolicitud)
		w.Header().Set("Content-Type", "application/json")
		fmt.Fprintf(w, `{"id":%q,"titulo":"T","copiasTotales":1,"copiasDisponibles":1}`, idLibro)
	}))
	defer srv.Close()

	cli := NuevoClienteHTTP(srv.URL, secretoPrueba, 2*time.Second).SinReintentos()

	err := cli.ValidarLibroDisponible(context.Background(), idLibro)
	require.NoError(t, err)
	assert.Empty(t, idRecibido, "sin id en contexto el cliente no debe enviar X-Request-Id")
}
