package dominio

type EstadoPrestamo string

const (
	EstadoActivo   EstadoPrestamo = "ACTIVO"
	EstadoDevuelto EstadoPrestamo = "DEVUELTO"
)

func (e EstadoPrestamo) Valido() bool {
	switch e {
	case EstadoActivo, EstadoDevuelto:
		return true
	default:
		return false
	}
}
