package controladores

import (
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"strconv"
	"time"

	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/aplicacion"
	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/infraestructura/web/dto"
	"github.com/google/uuid"
)

const formatoFecha = "2006-01-02"

type ManejadorError func(w http.ResponseWriter, r *http.Request, err error)

var ErrSolicitudInvalida = errors.New("solicitud invalida")

type ControladorPrestamos struct {
	registrar      *aplicacion.RegistrarPrestamo
	devolver       *aplicacion.DevolverPrestamo
	consultar      *aplicacion.ConsultarPrestamo
	listarPorU     *aplicacion.ListarPrestamosPorUsuario
	listar         *aplicacion.ListarPrestamos
	manejadorError ManejadorError
}

func Nuevo(
	registrar *aplicacion.RegistrarPrestamo,
	devolver *aplicacion.DevolverPrestamo,
	consultar *aplicacion.ConsultarPrestamo,
	listarPorU *aplicacion.ListarPrestamosPorUsuario,
	listar *aplicacion.ListarPrestamos,
	manejadorError ManejadorError,
) *ControladorPrestamos {
	return &ControladorPrestamos{
		registrar:      registrar,
		devolver:       devolver,
		consultar:      consultar,
		listarPorU:     listarPorU,
		listar:         listar,
		manejadorError: manejadorError,
	}
}

func (c *ControladorPrestamos) Registrar(w http.ResponseWriter, r *http.Request) {
	var solicitud dto.RegistrarPrestamoSolicitud
	dec := json.NewDecoder(r.Body)
	dec.DisallowUnknownFields()
	if err := dec.Decode(&solicitud); err != nil {
		c.manejadorError(w, r, fmt.Errorf("%w: cuerpo JSON invalido: %v", ErrSolicitudInvalida, err))
		return
	}

	idUsuario, err := parsearUUID("idUsuario", solicitud.IDUsuario)
	if err != nil {
		c.manejadorError(w, r, err)
		return
	}
	idLibro, err := parsearUUID("idLibro", solicitud.IDLibro)
	if err != nil {
		c.manejadorError(w, r, err)
		return
	}
	fp, err := parsearFecha("fechaPrestamo", solicitud.FechaPrestamo)
	if err != nil {
		c.manejadorError(w, r, err)
		return
	}
	fde, err := parsearFecha("fechaDevolucionEstimada", solicitud.FechaDevolucionEstimada)
	if err != nil {
		c.manejadorError(w, r, err)
		return
	}

	prestamo, err := c.registrar.Ejecutar(r.Context(), aplicacion.RegistrarPrestamoComando{
		IDUsuario:               idUsuario,
		IDLibro:                 idLibro,
		FechaPrestamo:           fp,
		FechaDevolucionEstimada: fde,
	})
	if err != nil {
		c.manejadorError(w, r, err)
		return
	}
	responderJSON(w, http.StatusCreated, dto.DesdeDominio(prestamo))
}

func (c *ControladorPrestamos) Devolver(w http.ResponseWriter, r *http.Request) {
	id, err := parsearUUID("id", r.PathValue("id"))
	if err != nil {
		c.manejadorError(w, r, err)
		return
	}
	prestamo, err := c.devolver.Ejecutar(r.Context(), aplicacion.DevolverPrestamoComando{ID: id})
	if err != nil {
		c.manejadorError(w, r, err)
		return
	}
	responderJSON(w, http.StatusOK, dto.DesdeDominio(prestamo))
}

func (c *ControladorPrestamos) Consultar(w http.ResponseWriter, r *http.Request) {
	id, err := parsearUUID("id", r.PathValue("id"))
	if err != nil {
		c.manejadorError(w, r, err)
		return
	}
	prestamo, err := c.consultar.Ejecutar(r.Context(), id)
	if err != nil {
		c.manejadorError(w, r, err)
		return
	}
	responderJSON(w, http.StatusOK, dto.DesdeDominio(prestamo))
}

func (c *ControladorPrestamos) ListarPorUsuario(w http.ResponseWriter, r *http.Request) {
	idUsuario, err := parsearUUID("idUsuario", r.PathValue("idUsuario"))
	if err != nil {
		c.manejadorError(w, r, err)
		return
	}
	pagina, tamano := parsearPaginacion(r)
	pag, err := c.listarPorU.Ejecutar(r.Context(), idUsuario, pagina, tamano)
	if err != nil {
		c.manejadorError(w, r, err)
		return
	}
	responderJSON(w, http.StatusOK, dto.DesdePagina(pag))
}

func (c *ControladorPrestamos) Listar(w http.ResponseWriter, r *http.Request) {
	pagina, tamano := parsearPaginacion(r)
	pag, err := c.listar.Ejecutar(r.Context(), pagina, tamano)
	if err != nil {
		c.manejadorError(w, r, err)
		return
	}
	responderJSON(w, http.StatusOK, dto.DesdePagina(pag))
}

func parsearUUID(nombre, valor string) (uuid.UUID, error) {
	if valor == "" {
		return uuid.Nil, fmt.Errorf("%w: %s requerido", ErrSolicitudInvalida, nombre)
	}
	id, err := uuid.Parse(valor)
	if err != nil {
		return uuid.Nil, fmt.Errorf("%w: %s no es un UUID valido", ErrSolicitudInvalida, nombre)
	}
	return id, nil
}

func parsearFecha(nombre, valor string) (time.Time, error) {
	if valor == "" {
		return time.Time{}, fmt.Errorf("%w: %s requerido", ErrSolicitudInvalida, nombre)
	}
	t, err := time.Parse(formatoFecha, valor)
	if err != nil {
		return time.Time{}, fmt.Errorf("%w: %s debe tener formato YYYY-MM-DD", ErrSolicitudInvalida, nombre)
	}
	return t, nil
}

func parsearPaginacion(r *http.Request) (int, int) {
	pagina := 0
	tamano := 20
	if v := r.URL.Query().Get("pagina"); v != "" {
		if n, err := strconv.Atoi(v); err == nil {
			pagina = n
		}
	}
	if v := r.URL.Query().Get("tamano"); v != "" {
		if n, err := strconv.Atoi(v); err == nil {
			tamano = n
		}
	}
	return pagina, tamano
}

func responderJSON(w http.ResponseWriter, estado int, cuerpo any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(estado)
	_ = json.NewEncoder(w).Encode(cuerpo)
}
