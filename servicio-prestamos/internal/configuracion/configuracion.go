package configuracion

import (
	"fmt"
	"net/url"
	"os"
	"strconv"
	"strings"
	"time"
)

type Configuracion struct {
	PuertoHTTP             int
	BaseDeDatos            string
	URLServicioBiblioteca  string
	SecretoServicioInterno string
	TimeoutClienteHTTP     time.Duration
}

func Cargar() (Configuracion, error) {
	cfg := Configuracion{
		PuertoHTTP:             obtenerEntero("SERVICIO_PRESTAMOS_PUERTO", 8081),
		BaseDeDatos:            construirDSN(),
		URLServicioBiblioteca:  obtenerCadena("SERVICIO_BIBLIOTECA_URL", "http://servicio-biblioteca:8080"),
		SecretoServicioInterno: obtenerCadena("SERVICIO_INTERNO_SECRETO", "secreto-compartido-mariposa-cambiar-en-produccion"),
		TimeoutClienteHTTP:     time.Duration(obtenerEntero("HTTP_CLIENTE_TIMEOUT_SEGUNDOS", 5)) * time.Second,
	}

	if cfg.PuertoHTTP <= 0 || cfg.PuertoHTTP > 65535 {
		return Configuracion{}, fmt.Errorf("puerto HTTP invalido: %d", cfg.PuertoHTTP)
	}
	if strings.TrimSpace(cfg.URLServicioBiblioteca) == "" {
		return Configuracion{}, fmt.Errorf("SERVICIO_BIBLIOTECA_URL no puede estar vacio")
	}
	if _, err := url.Parse(cfg.URLServicioBiblioteca); err != nil {
		return Configuracion{}, fmt.Errorf("SERVICIO_BIBLIOTECA_URL invalido: %w", err)
	}
	if strings.TrimSpace(cfg.SecretoServicioInterno) == "" {
		return Configuracion{}, fmt.Errorf("SERVICIO_INTERNO_SECRETO no puede estar vacio")
	}
	if cfg.TimeoutClienteHTTP <= 0 {
		return Configuracion{}, fmt.Errorf("HTTP_CLIENTE_TIMEOUT_SEGUNDOS debe ser positivo")
	}
	return cfg, nil
}

func construirDSN() string {
	if dsn := os.Getenv("SERVICIO_PRESTAMOS_DSN"); dsn != "" {
		return dsn
	}
	host := obtenerCadena("POSTGRES_PRESTAMOS_HOST", "postgres-prestamos")
	puerto := obtenerEntero("POSTGRES_PRESTAMOS_PORT", 5432)
	base := obtenerCadena("POSTGRES_PRESTAMOS_DB", "prestamos")
	usuario := obtenerCadena("POSTGRES_PRESTAMOS_USER", "prestamos_app")
	clave := obtenerCadena("POSTGRES_PRESTAMOS_PASSWORD", "cambiar_en_local")
	sslmode := obtenerCadena("POSTGRES_PRESTAMOS_SSLMODE", "disable")
	return fmt.Sprintf("postgres://%s:%s@%s:%d/%s?sslmode=%s",
		url.QueryEscape(usuario), url.QueryEscape(clave), host, puerto, base, sslmode)
}

func obtenerCadena(clave, defecto string) string {
	if v, ok := os.LookupEnv(clave); ok && v != "" {
		return v
	}
	return defecto
}

func obtenerEntero(clave string, defecto int) int {
	if v, ok := os.LookupEnv(clave); ok && v != "" {
		if n, err := strconv.Atoi(v); err == nil {
			return n
		}
	}
	return defecto
}
