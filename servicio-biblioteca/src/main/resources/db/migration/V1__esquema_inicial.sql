CREATE TABLE IF NOT EXISTS usuarios (
    id UUID PRIMARY KEY,
    nombre_usuario VARCHAR(60) NOT NULL,
    correo_electronico VARCHAR(255) NOT NULL,
    contrasena_encriptada VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_usuarios_nombre_usuario UNIQUE (nombre_usuario),
    CONSTRAINT uq_usuarios_correo_electronico UNIQUE (correo_electronico),
    CONSTRAINT ck_usuarios_rol CHECK (rol IN ('ADMIN', 'USUARIO')),
    CONSTRAINT ck_usuarios_estado CHECK (estado IN ('ACTIVO', 'INACTIVO', 'BLOQUEADO'))
);

CREATE INDEX IF NOT EXISTS idx_usuarios_estado ON usuarios (estado);
CREATE INDEX IF NOT EXISTS idx_usuarios_rol ON usuarios (rol);

COMMENT ON TABLE usuarios IS 'Catálogo de usuarios autenticables del sistema de biblioteca';
COMMENT ON COLUMN usuarios.contrasena_encriptada IS 'Hash BCrypt de la contraseña; nunca debe almacenarse en texto plano';
COMMENT ON COLUMN usuarios.rol IS 'Rol funcional del usuario (ADMIN, USUARIO)';
COMMENT ON COLUMN usuarios.estado IS 'Estado del ciclo de vida del usuario (ACTIVO, INACTIVO, BLOQUEADO)';

CREATE TABLE IF NOT EXISTS libros (
    id UUID PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    autor VARCHAR(255) NOT NULL,
    isbn VARCHAR(13) NOT NULL,
    anio_publicacion INTEGER NOT NULL,
    genero VARCHAR(20) NOT NULL,
    copias_totales INTEGER NOT NULL,
    copias_disponibles INTEGER NOT NULL,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_libros_isbn UNIQUE (isbn),
    CONSTRAINT ck_libros_genero CHECK (
        genero IN ('FICCION', 'NO_FICCION', 'CIENCIA', 'HISTORIA',
                   'BIOGRAFIA', 'INFANTIL', 'TECNICO', 'OTRO')
    ),
    CONSTRAINT ck_libros_anio CHECK (anio_publicacion >= 1000),
    CONSTRAINT ck_libros_copias_totales CHECK (copias_totales >= 0),
    CONSTRAINT ck_libros_copias_disponibles CHECK (copias_disponibles >= 0),
    CONSTRAINT ck_libros_copias_consistencia CHECK (copias_disponibles <= copias_totales)
);

CREATE INDEX IF NOT EXISTS idx_libros_autor ON libros (autor);
CREATE INDEX IF NOT EXISTS idx_libros_genero ON libros (genero);
CREATE INDEX IF NOT EXISTS idx_libros_titulo ON libros (titulo);

COMMENT ON TABLE libros IS 'Catálogo de libros gestionados por el servicio de biblioteca';
COMMENT ON COLUMN libros.isbn IS 'Identificador estándar ISBN-10 o ISBN-13 sin separadores';
COMMENT ON COLUMN libros.copias_totales IS 'Cantidad total de copias físicas registradas para el libro';
COMMENT ON COLUMN libros.copias_disponibles IS 'Cantidad de copias actualmente disponibles para préstamo';
