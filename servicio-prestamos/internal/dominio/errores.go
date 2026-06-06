package dominio

import "errors"

var (
	ErrPrestamoNoEncontrado           = errors.New("prestamo no encontrado")
	ErrPrestamoYaDevuelto             = errors.New("prestamo ya devuelto")
	ErrFechaDevolucionInvalida        = errors.New("fecha de devolucion debe ser posterior a fecha de prestamo")
	ErrIDsInvalidos                   = errors.New("ids no pueden ser nulos")
	ErrLibroNoEncontrado              = errors.New("libro no encontrado en servicio biblioteca")
	ErrSinCopiasDisponibles           = errors.New("libro sin copias disponibles")
	ErrServicioBibliotecaNoDisponible = errors.New("servicio biblioteca no disponible")
)
