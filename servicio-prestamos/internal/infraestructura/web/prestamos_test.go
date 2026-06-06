package web

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/aplicacion"
	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/dominio"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

type repoMemoria struct {
	mu    sync.Mutex
	porID map[uuid.UUID]dominio.Prestamo
	orden []uuid.UUID
}

func nuevoRepoMemoria() *repoMemoria {
	return &repoMemoria{porID: map[uuid.UUID]dominio.Prestamo{}}
}

func (r *repoMemoria) Guardar(_ context.Context, p dominio.Prestamo) error {
	r.mu.Lock()
	defer r.mu.Unlock()
	if _, existe := r.porID[p.ID()]; !existe {
		r.orden = append(r.orden, p.ID())
	}
	r.porID[p.ID()] = p
	return nil
}

func (r *repoMemoria) ObtenerPorID(_ context.Context, id uuid.UUID) (dominio.Prestamo, error) {
	r.mu.Lock()
	defer r.mu.Unlock()
	p, ok := r.porID[id]
	if !ok {
		return dominio.Prestamo{}, dominio.ErrPrestamoNoEncontrado
	}
	return p, nil
}

func (r *repoMemoria) ListarPorUsuario(_ context.Context, idUsuario uuid.UUID, pagina, tamano int) (aplicacion.Pagina[dominio.Prestamo], error) {
	r.mu.Lock()
	defer r.mu.Unlock()
	var todos []dominio.Prestamo
	for _, id := range r.orden {
		p := r.porID[id]
		if p.IDUsuario() == idUsuario {
			todos = append(todos, p)
		}
	}
	return paginar(todos, pagina, tamano), nil
}

func (r *repoMemoria) Listar(_ context.Context, pagina, tamano int) (aplicacion.Pagina[dominio.Prestamo], error) {
	r.mu.Lock()
	defer r.mu.Unlock()
	var todos []dominio.Prestamo
	for _, id := range r.orden {
		todos = append(todos, r.porID[id])
	}
	return paginar(todos, pagina, tamano), nil
}

func paginar(todos []dominio.Prestamo, pagina, tamano int) aplicacion.Pagina[dominio.Prestamo] {
	total := int64(len(todos))
	desde := pagina * tamano
	hasta := desde + tamano
	if desde > len(todos) {
		desde = len(todos)
	}
	if hasta > len(todos) {
		hasta = len(todos)
	}
	totalPaginas := 0
	if tamano > 0 {
		totalPaginas = int((total + int64(tamano) - 1) / int64(tamano))
	}
	return aplicacion.Pagina[dominio.Prestamo]{
		Elementos:      todos[desde:hasta],
		PaginaActual:   pagina,
		TamanoPagina:   tamano,
		TotalElementos: total,
		TotalPaginas:   totalPaginas,
	}
}

type clienteStub struct {
	err error
}

func (c *clienteStub) ValidarLibroDisponible(_ context.Context, _ uuid.UUID) error { return c.err }

func nuevoSrvHTTP(t *testing.T, cliente aplicacion.ClienteBiblioteca) (*httptest.Server, *repoMemoria) {
	t.Helper()
	repo := nuevoRepoMemoria()
	registrar := aplicacion.NuevoRegistrarPrestamo(repo, cliente)
	devolver := aplicacion.NuevoDevolverPrestamo(repo)
	consultar := aplicacion.NuevoConsultarPrestamo(repo)
	listarPorU := aplicacion.NuevoListarPrestamosPorUsuario(repo)
	listar := aplicacion.NuevoListarPrestamos(repo)
	srv := NuevoServidor(registrar, devolver, consultar, listarPorU, listar)
	return httptest.NewServer(srv.Handler()), repo
}

func cuerpoValido() string {
	idU := uuid.NewString()
	idL := uuid.NewString()
	return `{
        "idUsuario": "` + idU + `",
        "idLibro": "` + idL + `",
        "fechaPrestamo": "2026-06-01",
        "fechaDevolucionEstimada": "2026-06-15"
    }`
}

func TestPOST_RegistrarPrestamo_Exitoso(t *testing.T) {
	srv, _ := nuevoSrvHTTP(t, &clienteStub{})
	defer srv.Close()

	resp, err := http.Post(srv.URL+"/api/v1/prestamos", "application/json", strings.NewReader(cuerpoValido()))
	require.NoError(t, err)
	defer resp.Body.Close()

	assert.Equal(t, http.StatusCreated, resp.StatusCode)
	assert.NotEmpty(t, resp.Header.Get("X-Request-Id"))

	var cuerpo map[string]any
	require.NoError(t, json.NewDecoder(resp.Body).Decode(&cuerpo))
	assert.Equal(t, "ACTIVO", cuerpo["estado"])
	assert.Equal(t, false, cuerpo["estaAtrasado"])
}

func TestPOST_RegistrarPrestamo_LibroNoExiste(t *testing.T) {
	srv, _ := nuevoSrvHTTP(t, &clienteStub{err: dominio.ErrLibroNoEncontrado})
	defer srv.Close()

	resp, err := http.Post(srv.URL+"/api/v1/prestamos", "application/json", strings.NewReader(cuerpoValido()))
	require.NoError(t, err)
	defer resp.Body.Close()

	assert.Equal(t, http.StatusUnprocessableEntity, resp.StatusCode)
	assert.Equal(t, "application/problem+json", resp.Header.Get("Content-Type"))
}

func TestPOST_RegistrarPrestamo_SinCopias(t *testing.T) {
	srv, _ := nuevoSrvHTTP(t, &clienteStub{err: dominio.ErrSinCopiasDisponibles})
	defer srv.Close()

	resp, err := http.Post(srv.URL+"/api/v1/prestamos", "application/json", strings.NewReader(cuerpoValido()))
	require.NoError(t, err)
	defer resp.Body.Close()

	assert.Equal(t, http.StatusConflict, resp.StatusCode)
}

func TestPOST_RegistrarPrestamo_JSONInvalido(t *testing.T) {
	srv, _ := nuevoSrvHTTP(t, &clienteStub{})
	defer srv.Close()

	resp, err := http.Post(srv.URL+"/api/v1/prestamos", "application/json", bytes.NewReader([]byte("{")))
	require.NoError(t, err)
	defer resp.Body.Close()
	assert.Equal(t, http.StatusUnprocessableEntity, resp.StatusCode)
}

func TestPOST_RegistrarPrestamo_FechaMalFormada(t *testing.T) {
	srv, _ := nuevoSrvHTTP(t, &clienteStub{})
	defer srv.Close()

	cuerpo := `{
        "idUsuario": "` + uuid.NewString() + `",
        "idLibro": "` + uuid.NewString() + `",
        "fechaPrestamo": "01-06-2026",
        "fechaDevolucionEstimada": "2026-06-15"
    }`
	resp, err := http.Post(srv.URL+"/api/v1/prestamos", "application/json", strings.NewReader(cuerpo))
	require.NoError(t, err)
	defer resp.Body.Close()
	assert.Equal(t, http.StatusUnprocessableEntity, resp.StatusCode)
}

func TestGET_ConsultarPrestamo_NoEncontrado(t *testing.T) {
	srv, _ := nuevoSrvHTTP(t, &clienteStub{})
	defer srv.Close()

	resp, err := http.Get(srv.URL + "/api/v1/prestamos/" + uuid.NewString())
	require.NoError(t, err)
	defer resp.Body.Close()
	assert.Equal(t, http.StatusNotFound, resp.StatusCode)
}

func TestPOST_DevolverPrestamo_Flujo(t *testing.T) {
	srv, repo := nuevoSrvHTTP(t, &clienteStub{})
	defer srv.Close()

	p, err := dominio.NuevoPrestamo(uuid.New(), uuid.New(),
		time.Date(2026, 6, 1, 0, 0, 0, 0, time.UTC),
		time.Date(2026, 6, 15, 0, 0, 0, 0, time.UTC))
	require.NoError(t, err)
	require.NoError(t, repo.Guardar(context.Background(), p))

	resp, err := http.Post(srv.URL+"/api/v1/prestamos/"+p.ID().String()+"/devolver", "application/json", nil)
	require.NoError(t, err)
	defer resp.Body.Close()
	assert.Equal(t, http.StatusOK, resp.StatusCode)

	var cuerpo map[string]any
	require.NoError(t, json.NewDecoder(resp.Body).Decode(&cuerpo))
	assert.Equal(t, "DEVUELTO", cuerpo["estado"])

	resp2, err := http.Post(srv.URL+"/api/v1/prestamos/"+p.ID().String()+"/devolver", "application/json", nil)
	require.NoError(t, err)
	defer resp2.Body.Close()
	assert.Equal(t, http.StatusConflict, resp2.StatusCode)
}

func TestGET_ListarPrestamos(t *testing.T) {
	srv, repo := nuevoSrvHTTP(t, &clienteStub{})
	defer srv.Close()

	for i := 0; i < 3; i++ {
		p, err := dominio.NuevoPrestamo(uuid.New(), uuid.New(),
			time.Date(2026, 6, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2026, 6, 15, 0, 0, 0, 0, time.UTC))
		require.NoError(t, err)
		require.NoError(t, repo.Guardar(context.Background(), p))
	}

	resp, err := http.Get(srv.URL + "/api/v1/prestamos")
	require.NoError(t, err)
	defer resp.Body.Close()
	assert.Equal(t, http.StatusOK, resp.StatusCode)

	var cuerpo map[string]any
	require.NoError(t, json.NewDecoder(resp.Body).Decode(&cuerpo))
	assert.Equal(t, float64(3), cuerpo["totalElementos"])
}

func TestGET_Salud(t *testing.T) {
	srv, _ := nuevoSrvHTTP(t, &clienteStub{})
	defer srv.Close()

	resp, err := http.Get(srv.URL + "/salud")
	require.NoError(t, err)
	defer resp.Body.Close()
	assert.Equal(t, http.StatusOK, resp.StatusCode)
}
