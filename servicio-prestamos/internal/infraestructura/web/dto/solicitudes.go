package dto

type RegistrarPrestamoSolicitud struct {
	IDUsuario               string `json:"idUsuario"`
	IDLibro                 string `json:"idLibro"`
	FechaPrestamo           string `json:"fechaPrestamo"`
	FechaDevolucionEstimada string `json:"fechaDevolucionEstimada"`
}
