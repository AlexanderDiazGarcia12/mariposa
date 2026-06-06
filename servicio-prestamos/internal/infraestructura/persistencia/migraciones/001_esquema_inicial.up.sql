CREATE TABLE IF NOT EXISTS prestamos (
    id UUID PRIMARY KEY,
    id_usuario UUID NOT NULL,
    id_libro UUID NOT NULL,
    fecha_prestamo DATE NOT NULL,
    fecha_devolucion_estimada DATE NOT NULL,
    fecha_devolucion_real DATE,
    estado VARCHAR(20) NOT NULL,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_prestamos_estado CHECK (estado IN ('ACTIVO', 'DEVUELTO')),
    CONSTRAINT ck_prestamos_fechas CHECK (fecha_devolucion_estimada > fecha_prestamo)
);

CREATE INDEX IF NOT EXISTS idx_prestamos_usuario ON prestamos (id_usuario);
CREATE INDEX IF NOT EXISTS idx_prestamos_libro ON prestamos (id_libro);
CREATE INDEX IF NOT EXISTS idx_prestamos_estado ON prestamos (estado);

COMMENT ON TABLE prestamos IS 'Registro de prestamos de libros - Servicio B';
