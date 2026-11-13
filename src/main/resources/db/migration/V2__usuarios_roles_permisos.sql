-- V2: usuarios, roles, permisos y registro de accesos.

CREATE TABLE rol (
    id          INTEGER PRIMARY KEY,
    codigo      TEXT NOT NULL UNIQUE,
    nombre      TEXT NOT NULL,
    descripcion TEXT NOT NULL
);

CREATE TABLE permiso (
    codigo      TEXT PRIMARY KEY,
    descripcion TEXT NOT NULL
);

CREATE TABLE rol_permiso (
    rol_id         INTEGER NOT NULL REFERENCES rol (id) ON DELETE CASCADE,
    permiso_codigo TEXT    NOT NULL REFERENCES permiso (codigo) ON DELETE CASCADE,
    PRIMARY KEY (rol_id, permiso_codigo)
);

CREATE TABLE usuario (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre_usuario        TEXT    NOT NULL UNIQUE COLLATE NOCASE,
    nombre_completo       TEXT    NOT NULL,
    email                 TEXT,
    password_hash         TEXT    NOT NULL,
    rol_id                INTEGER NOT NULL REFERENCES rol (id),
    activo                INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0, 1)),
    debe_cambiar_password INTEGER NOT NULL DEFAULT 0 CHECK (debe_cambiar_password IN (0, 1)),
    intentos_fallidos     INTEGER NOT NULL DEFAULT 0,
    bloqueado_hasta       TEXT,
    ultimo_acceso         TEXT,
    creado_en             TEXT    NOT NULL,
    actualizado_en        TEXT    NOT NULL
);

CREATE INDEX idx_usuario_rol ON usuario (rol_id);

CREATE TABLE registro_acceso (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id     INTEGER REFERENCES usuario (id),
    nombre_usuario TEXT    NOT NULL,
    evento         TEXT    NOT NULL,
    realizado_por  TEXT,
    fecha          TEXT    NOT NULL
);

CREATE INDEX idx_registro_acceso_fecha ON registro_acceso (fecha);

-- Roles predefinidos
INSERT INTO rol (id, codigo, nombre, descripcion) VALUES
    (1, 'ADMINISTRADOR', 'Administrador', 'Acceso total, incluida la gestión de usuarios y la configuración'),
    (2, 'GERENTE', 'Gerente', 'Registra información y consulta todos los análisis e informes'),
    (3, 'ANALISTA', 'Analista', 'Consulta información, análisis e informes sin modificar datos'),
    (4, 'OPERADOR', 'Operador', 'Registra productos, ventas, compras y movimientos financieros');

-- Permisos (deben coincidir con el enum com.businessanalyzer.model.Permiso)
INSERT INTO permiso (codigo, descripcion) VALUES
    ('DASHBOARD_VER', 'Ver el dashboard'),
    ('EMPRESAS_VER', 'Ver empresas'),
    ('EMPRESAS_GESTIONAR', 'Registrar y modificar empresas'),
    ('PRODUCTOS_VER', 'Ver productos y servicios'),
    ('PRODUCTOS_GESTIONAR', 'Registrar y modificar productos y servicios'),
    ('OPERACIONES_VER', 'Ver ventas y compras'),
    ('OPERACIONES_GESTIONAR', 'Registrar y modificar ventas y compras'),
    ('FINANZAS_VER', 'Ver ingresos, gastos y costos'),
    ('FINANZAS_GESTIONAR', 'Registrar y modificar ingresos, gastos y costos'),
    ('ANALISIS_VER', 'Ver análisis, predicciones, escenarios y diagnóstico'),
    ('INFORMES_GENERAR', 'Generar y exportar informes'),
    ('USUARIOS_GESTIONAR', 'Administrar usuarios y revisar accesos'),
    ('CONFIGURACION_GESTIONAR', 'Configuración y copias de seguridad');

-- Administrador: todos los permisos
INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT 1, codigo FROM permiso;

-- Gerente: todo excepto usuarios y configuración
INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT 2, codigo FROM permiso
WHERE codigo NOT IN ('USUARIOS_GESTIONAR', 'CONFIGURACION_GESTIONAR');

-- Analista: solo lectura, análisis e informes
INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT 3, codigo FROM permiso
WHERE codigo IN ('DASHBOARD_VER', 'EMPRESAS_VER', 'PRODUCTOS_VER', 'OPERACIONES_VER',
                 'FINANZAS_VER', 'ANALISIS_VER', 'INFORMES_GENERAR');

-- Operador: registro de datos, sin análisis
INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT 4, codigo FROM permiso
WHERE codigo IN ('DASHBOARD_VER', 'EMPRESAS_VER', 'PRODUCTOS_VER', 'PRODUCTOS_GESTIONAR',
                 'OPERACIONES_VER', 'OPERACIONES_GESTIONAR', 'FINANZAS_VER', 'FINANZAS_GESTIONAR');
