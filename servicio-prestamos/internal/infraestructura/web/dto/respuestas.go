package dto

import (
	"time"

	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/aplicacion"
	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/dominio"
)

const formatoFecha = "2006-01-02"

type PrestamoRespuesta struct {
	ID                      string  `json:"id"`
	IDUsuario               string  `json:"idUsuario"`
	IDLibro                 string  `json:"idLibro"`
	FechaPrestamo           string  `json:"fechaPrestamo"`
	FechaDevolucionEstimada string  `json:"fechaDevolucionEstimada"`
	FechaDevolucionReal     *string `json:"fechaDevolucionReal,omitempty"`
	Estado                  string  `json:"estado"`
	EstaAtrasado            bool    `json:"estaAtrasado"`
	FechaCreacion           string  `json:"fechaCreacion"`
	FechaActualizacion      string  `json:"fechaActualizacion"`
}

func DesdeDominio(p dominio.Prestamo) PrestamoRespuesta {
	var fdr *string
	if p.FechaDevolucionReal() != nil {
		s := p.FechaDevolucionReal().Format(formatoFecha)
		fdr = &s
	}
	return PrestamoRespuesta{
		ID:                      p.ID().String(),
		IDUsuario:               p.IDUsuario().String(),
		IDLibro:                 p.IDLibro().String(),
		FechaPrestamo:           p.FechaPrestamo().Format(formatoFecha),
		FechaDevolucionEstimada: p.FechaDevolucionEstimada().Format(formatoFecha),
		FechaDevolucionReal:     fdr,
		Estado:                  string(p.Estado()),
		EstaAtrasado:            p.EstaAtrasado(time.Now().UTC()),
		FechaCreacion:           p.FechaCreacion().UTC().Format(time.RFC3339),
		FechaActualizacion:      p.FechaActualizacion().UTC().Format(time.RFC3339),
	}
}

type PaginaPrestamosRespuesta struct {
	Elementos      []PrestamoRespuesta `json:"elementos"`
	PaginaActual   int                 `json:"paginaActual"`
	TamanoPagina   int                 `json:"tamanoPagina"`
	TotalElementos int64               `json:"totalElementos"`
	TotalPaginas   int                 `json:"totalPaginas"`
}

func DesdePagina(p aplicacion.Pagina[dominio.Prestamo]) PaginaPrestamosRespuesta {
	elementos := make([]PrestamoRespuesta, 0, len(p.Elementos))
	for _, prestamo := range p.Elementos {
		elementos = append(elementos, DesdeDominio(prestamo))
	}
	return PaginaPrestamosRespuesta{
		Elementos:      elementos,
		PaginaActual:   p.PaginaActual,
		TamanoPagina:   p.TamanoPagina,
		TotalElementos: p.TotalElementos,
		TotalPaginas:   p.TotalPaginas,
	}
}
