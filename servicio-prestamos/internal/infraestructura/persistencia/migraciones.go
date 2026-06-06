package persistencia

import (
	"embed"
	"errors"
	"fmt"

	"github.com/golang-migrate/migrate/v4"
	_ "github.com/golang-migrate/migrate/v4/database/pgx/v5"
	"github.com/golang-migrate/migrate/v4/source/iofs"
)

//go:embed migraciones/*.sql
var archivosMigraciones embed.FS

func AplicarMigraciones(dsn string) error {
	fuente, err := iofs.New(archivosMigraciones, "migraciones")
	if err != nil {
		return fmt.Errorf("preparando fuente iofs: %w", err)
	}

	url := convertirDSN(dsn)

	m, err := migrate.NewWithSourceInstance("iofs", fuente, url)
	if err != nil {
		return fmt.Errorf("creando migrate: %w", err)
	}
	defer m.Close()

	if err := m.Up(); err != nil && !errors.Is(err, migrate.ErrNoChange) {
		return fmt.Errorf("aplicando migraciones: %w", err)
	}
	return nil
}

func convertirDSN(dsn string) string {
	const prefijoOriginal1 = "postgres://"
	const prefijoOriginal2 = "postgresql://"
	const prefijoNuevo = "pgx5://"
	switch {
	case len(dsn) >= len(prefijoOriginal1) && dsn[:len(prefijoOriginal1)] == prefijoOriginal1:
		return prefijoNuevo + dsn[len(prefijoOriginal1):]
	case len(dsn) >= len(prefijoOriginal2) && dsn[:len(prefijoOriginal2)] == prefijoOriginal2:
		return prefijoNuevo + dsn[len(prefijoOriginal2):]
	default:
		return dsn
	}
}
