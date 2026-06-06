-- Usuario administrador semilla destinado únicamente a entornos de prueba.
-- Permite realizar pruebas manuales (login, creación de libros, etc.)
-- sin tener que registrar un usuario por API y promoverlo manualmente en base de datos.
--
-- Credenciales de prueba:
--   nombre_usuario: admin
--   contrasena   : Admin123!
--
-- El hash BCrypt fue generado con BCryptPasswordEncoder (Spring Security)
-- cost factor 10 y verificado con matches() == true antes de incluirse aquí.
-- En entornos productivos esta semilla debe deshabilitarse o regenerarse.

INSERT INTO usuarios (
    id,
    nombre_usuario,
    correo_electronico,
    contrasena_encriptada,
    rol,
    estado,
    fecha_creacion,
    fecha_actualizacion
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin',
    'admin@mariposa.local',
    '$2a$10$YxrULRBGpFCkSgwZp2e3Pe9vCGJqi4dfM1F6Iz29Nbd2k6o1aRzAS',
    'ADMIN',
    'ACTIVO',
    NOW(),
    NOW()
)
ON CONFLICT (nombre_usuario) DO NOTHING;
