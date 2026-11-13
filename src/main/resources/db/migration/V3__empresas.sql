-- V3: empresas y acceso de usuarios a cada empresa.

CREATE TABLE empresa (
    id                       INTEGER PRIMARY KEY AUTOINCREMENT,
    razon_social             TEXT    NOT NULL,
    nombre_comercial         TEXT,
    identificacion_fiscal    TEXT    UNIQUE COLLATE NOCASE,
    sector                   TEXT    NOT NULL,
    tamano                   TEXT    NOT NULL,
    moneda                   TEXT    NOT NULL,
    mes_inicio_ejercicio     INTEGER NOT NULL DEFAULT 1 CHECK (mes_inicio_ejercicio BETWEEN 1 AND 12),
    fecha_inicio_operaciones TEXT,
    direccion                TEXT,
    telefono                 TEXT,
    email                    TEXT,
    activa                   INTEGER NOT NULL DEFAULT 1 CHECK (activa IN (0, 1)),
    creado_en                TEXT    NOT NULL,
    actualizado_en           TEXT    NOT NULL
);

-- Usuarios (no administradores) que pueden trabajar con cada empresa.
-- Los administradores acceden a todas las empresas sin necesidad de asignación.
CREATE TABLE usuario_empresa (
    usuario_id INTEGER NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    empresa_id INTEGER NOT NULL REFERENCES empresa (id) ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, empresa_id)
);

CREATE INDEX idx_usuario_empresa_empresa ON usuario_empresa (empresa_id);
