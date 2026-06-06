package cliente_biblioteca

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log/slog"
	"net/http"
	"net/url"
	"strings"
	"time"

	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/dominio"
	"github.com/google/uuid"
)

const nombreHeaderInterno = "X-Servicio-Interno"

const rutaInterna = "/api/v1/internal/libros/"

type LibroRespuestaInterna struct {
	ID                string `json:"id"`
	Titulo            string `json:"titulo"`
	Autor             string `json:"autor"`
	ISBN              string `json:"isbn"`
	AnioPublicacion   int    `json:"anioPublicacion"`
	Genero            string `json:"genero"`
	CopiasTotales     int    `json:"copiasTotales"`
	CopiasDisponibles int    `json:"copiasDisponibles"`
}

type ClienteBibliotecaHTTP struct {
	urlBase    string
	secreto    string
	cliente    *http.Client
	reintentos int
}

func NuevoClienteHTTP(urlBase, secreto string, timeout time.Duration) *ClienteBibliotecaHTTP {
	return &ClienteBibliotecaHTTP{
		urlBase:    strings.TrimRight(urlBase, "/"),
		secreto:    secreto,
		cliente:    &http.Client{Timeout: timeout},
		reintentos: 2,
	}
}

func (c *ClienteBibliotecaHTTP) ConHTTPClient(cli *http.Client) *ClienteBibliotecaHTTP {
	c.cliente = cli
	return c
}

func (c *ClienteBibliotecaHTTP) SinReintentos() *ClienteBibliotecaHTTP {
	c.reintentos = 0
	return c
}

var retardosReintento = []time.Duration{200 * time.Millisecond, 500 * time.Millisecond}

func (c *ClienteBibliotecaHTTP) ValidarLibroDisponible(ctx context.Context, idLibro uuid.UUID) error {
	destino, err := url.JoinPath(c.urlBase, rutaInterna, idLibro.String())
	if err != nil {
		return fmt.Errorf("construyendo url: %w", err)
	}

	var ultimoErr error
	intentos := c.reintentos + 1
	for intento := 0; intento < intentos; intento++ {
		if intento > 0 {
			retardo := retardosReintento[min(intento-1, len(retardosReintento)-1)]
			select {
			case <-ctx.Done():
				return fmt.Errorf("%w: %v", dominio.ErrServicioBibliotecaNoDisponible, ctx.Err())
			case <-time.After(retardo):
			}
		}

		err := c.ejecutarSolicitud(ctx, destino, idLibro)
		if err == nil {
			return nil
		}
		ultimoErr = err

		if !esReintentable(err) {
			return err
		}
		slog.Warn("reintentando llamada a servicio biblioteca",
			"intento", intento+1,
			"id_libro", idLibro.String(),
			"error", err.Error(),
		)
	}
	return ultimoErr
}

func (c *ClienteBibliotecaHTTP) ejecutarSolicitud(ctx context.Context, destino string, idLibro uuid.UUID) error {
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, destino, nil)
	if err != nil {
		return fmt.Errorf("creando request: %w", err)
	}
	req.Header.Set(nombreHeaderInterno, c.secreto)
	req.Header.Set("Accept", "application/json")

	resp, err := c.cliente.Do(req)
	if err != nil {
		slog.Warn("fallo de red llamando a servicio biblioteca",
			"id_libro", idLibro.String(), "error", err.Error())
		return fmt.Errorf("%w: %v", dominio.ErrServicioBibliotecaNoDisponible, err)
	}
	defer resp.Body.Close()

	switch {
	case resp.StatusCode == http.StatusOK:
		var libro LibroRespuestaInterna
		if err := json.NewDecoder(resp.Body).Decode(&libro); err != nil {
			return fmt.Errorf("%w: decodificando respuesta: %v", dominio.ErrServicioBibliotecaNoDisponible, err)
		}
		if libro.CopiasDisponibles <= 0 {
			return dominio.ErrSinCopiasDisponibles
		}
		return nil
	case resp.StatusCode == http.StatusNotFound:
		return dominio.ErrLibroNoEncontrado
	case resp.StatusCode == http.StatusUnauthorized || resp.StatusCode == http.StatusForbidden:
		slog.Warn("servicio biblioteca rechazo autenticacion interna",
			"status", resp.StatusCode, "id_libro", idLibro.String(),
			"nota", "el endpoint /api/v1/internal/libros/{id} puede no existir aun en el servicio A")
		return fmt.Errorf("%w: status %d (autenticacion interna)", dominio.ErrServicioBibliotecaNoDisponible, resp.StatusCode)
	case resp.StatusCode >= 500:
		cuerpo, _ := io.ReadAll(resp.Body)
		return fmt.Errorf("%w: status %d: %s", dominio.ErrServicioBibliotecaNoDisponible, resp.StatusCode, truncar(string(cuerpo), 200))
	default:
		return fmt.Errorf("%w: status inesperado %d", dominio.ErrServicioBibliotecaNoDisponible, resp.StatusCode)
	}
}

func esReintentable(err error) bool {
	return errors.Is(err, dominio.ErrServicioBibliotecaNoDisponible)
}

func truncar(s string, n int) string {
	if len(s) <= n {
		return s
	}
	return s[:n] + "..."
}

func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}
