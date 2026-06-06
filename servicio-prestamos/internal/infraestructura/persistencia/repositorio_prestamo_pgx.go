package persistencia

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/aplicacion"
	"github.com/AlexanderDiazGarcia12/mariposa/servicio-prestamos/internal/dominio"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type RepositorioPrestamoPgx struct {
	pool *pgxpool.Pool
}

func NuevoRepositorioPrestamoPgx(pool *pgxpool.Pool) *RepositorioPrestamoPgx {
	return &RepositorioPrestamoPgx{pool: pool}
}

const consultaUpsert = `
INSERT INTO prestamos (
    id, id_usuario, id_libro,
    fecha_prestamo, fecha_devolucion_estimada, fecha_devolucion_real,
    estado, fecha_creacion, fecha_actualizacion
) VALUES (
    $1, $2, $3, $4, $5, $6, $7, $8, $9
)
ON CONFLICT (id) DO UPDATE SET
    id_usuario = EXCLUDED.id_usuario,
    id_libro = EXCLUDED.id_libro,
    fecha_prestamo = EXCLUDED.fecha_prestamo,
    fecha_devolucion_estimada = EXCLUDED.fecha_devolucion_estimada,
    fecha_devolucion_real = EXCLUDED.fecha_devolucion_real,
    estado = EXCLUDED.estado,
    fecha_actualizacion = EXCLUDED.fecha_actualizacion
`

func (r *RepositorioPrestamoPgx) Guardar(ctx context.Context, p dominio.Prestamo) error {
	var fdr any
	if p.FechaDevolucionReal() != nil {
		fdr = *p.FechaDevolucionReal()
	} else {
		fdr = nil
	}
	_, err := r.pool.Exec(ctx, consultaUpsert,
		p.ID(), p.IDUsuario(), p.IDLibro(),
		p.FechaPrestamo(), p.FechaDevolucionEstimada(), fdr,
		string(p.Estado()), p.FechaCreacion(), p.FechaActualizacion(),
	)
	if err != nil {
		return fmt.Errorf("upsert prestamo: %w", err)
	}
	return nil
}

const consultaPorID = `
SELECT id, id_usuario, id_libro,
       fecha_prestamo, fecha_devolucion_estimada, fecha_devolucion_real,
       estado, fecha_creacion, fecha_actualizacion
FROM prestamos
WHERE id = $1
`

func (r *RepositorioPrestamoPgx) ObtenerPorID(ctx context.Context, id uuid.UUID) (dominio.Prestamo, error) {
	fila := r.pool.QueryRow(ctx, consultaPorID, id)
	p, err := escanearPrestamo(fila)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return dominio.Prestamo{}, dominio.ErrPrestamoNoEncontrado
		}
		return dominio.Prestamo{}, fmt.Errorf("consultando prestamo: %w", err)
	}
	return p, nil
}

const consultaListarPorUsuario = `
SELECT id, id_usuario, id_libro,
       fecha_prestamo, fecha_devolucion_estimada, fecha_devolucion_real,
       estado, fecha_creacion, fecha_actualizacion
FROM prestamos
WHERE id_usuario = $1
ORDER BY fecha_creacion DESC, id DESC
LIMIT $2 OFFSET $3
`

const consultaContarPorUsuario = `SELECT COUNT(*) FROM prestamos WHERE id_usuario = $1`

func (r *RepositorioPrestamoPgx) ListarPorUsuario(ctx context.Context, idUsuario uuid.UUID, pagina, tamano int) (aplicacion.Pagina[dominio.Prestamo], error) {
	var total int64
	if err := r.pool.QueryRow(ctx, consultaContarPorUsuario, idUsuario).Scan(&total); err != nil {
		return aplicacion.Pagina[dominio.Prestamo]{}, fmt.Errorf("contando prestamos: %w", err)
	}
	filas, err := r.pool.Query(ctx, consultaListarPorUsuario, idUsuario, tamano, pagina*tamano)
	if err != nil {
		return aplicacion.Pagina[dominio.Prestamo]{}, fmt.Errorf("listando prestamos: %w", err)
	}
	defer filas.Close()
	elementos, err := escanearFilas(filas)
	if err != nil {
		return aplicacion.Pagina[dominio.Prestamo]{}, err
	}
	return construirPagina(elementos, pagina, tamano, total), nil
}

const consultaListar = `
SELECT id, id_usuario, id_libro,
       fecha_prestamo, fecha_devolucion_estimada, fecha_devolucion_real,
       estado, fecha_creacion, fecha_actualizacion
FROM prestamos
ORDER BY fecha_creacion DESC, id DESC
LIMIT $1 OFFSET $2
`

const consultaContar = `SELECT COUNT(*) FROM prestamos`

func (r *RepositorioPrestamoPgx) Listar(ctx context.Context, pagina, tamano int) (aplicacion.Pagina[dominio.Prestamo], error) {
	var total int64
	if err := r.pool.QueryRow(ctx, consultaContar).Scan(&total); err != nil {
		return aplicacion.Pagina[dominio.Prestamo]{}, fmt.Errorf("contando prestamos: %w", err)
	}
	filas, err := r.pool.Query(ctx, consultaListar, tamano, pagina*tamano)
	if err != nil {
		return aplicacion.Pagina[dominio.Prestamo]{}, fmt.Errorf("listando prestamos: %w", err)
	}
	defer filas.Close()
	elementos, err := escanearFilas(filas)
	if err != nil {
		return aplicacion.Pagina[dominio.Prestamo]{}, err
	}
	return construirPagina(elementos, pagina, tamano, total), nil
}

type escaneable interface {
	Scan(dest ...any) error
}

func escanearPrestamo(s escaneable) (dominio.Prestamo, error) {
	var (
		id, idUsuario, idLibro uuid.UUID
		fp, fde                time.Time
		fdr                    *time.Time
		estado                 string
		fc, fa                 time.Time
	)
	if err := s.Scan(&id, &idUsuario, &idLibro, &fp, &fde, &fdr, &estado, &fc, &fa); err != nil {
		return dominio.Prestamo{}, err
	}
	return dominio.ReconstruirPrestamo(id, idUsuario, idLibro, fp, fde, fdr, dominio.EstadoPrestamo(estado), fc, fa), nil
}

func escanearFilas(filas pgx.Rows) ([]dominio.Prestamo, error) {
	var elementos []dominio.Prestamo
	for filas.Next() {
		p, err := escanearPrestamo(filas)
		if err != nil {
			return nil, fmt.Errorf("escaneando fila: %w", err)
		}
		elementos = append(elementos, p)
	}
	if err := filas.Err(); err != nil {
		return nil, fmt.Errorf("iterando filas: %w", err)
	}
	return elementos, nil
}

func construirPagina(elementos []dominio.Prestamo, pagina, tamano int, total int64) aplicacion.Pagina[dominio.Prestamo] {
	totalPaginas := 0
	if tamano > 0 {
		totalPaginas = int((total + int64(tamano) - 1) / int64(tamano))
	}
	if elementos == nil {
		elementos = []dominio.Prestamo{}
	}
	return aplicacion.Pagina[dominio.Prestamo]{
		Elementos:      elementos,
		PaginaActual:   pagina,
		TamanoPagina:   tamano,
		TotalElementos: total,
		TotalPaginas:   totalPaginas,
	}
}
