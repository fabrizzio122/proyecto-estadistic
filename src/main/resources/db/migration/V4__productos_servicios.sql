-- V4: catálogo de productos y servicios por empresa.
-- Los montos se guardan como INTEGER en diezmilésimas (valor x 10000): son exactos
-- (sin errores de redondeo de punto flotante) y se pueden sumar directamente en SQL.

CREATE TABLE categoria (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    empresa_id     INTEGER NOT NULL REFERENCES empresa (id),
    nombre         TEXT    NOT NULL COLLATE NOCASE,
    descripcion    TEXT,
    creado_en      TEXT    NOT NULL,
    actualizado_en TEXT    NOT NULL,
    UNIQUE (empresa_id, nombre)
);

CREATE TABLE producto (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    empresa_id     INTEGER NOT NULL REFERENCES empresa (id),
    categoria_id   INTEGER REFERENCES categoria (id),
    tipo           TEXT    NOT NULL CHECK (tipo IN ('PRODUCTO', 'SERVICIO')),
    codigo         TEXT    NOT NULL COLLATE NOCASE,
    nombre         TEXT    NOT NULL,
    descripcion    TEXT,
    unidad         TEXT    NOT NULL,
    costo_unitario INTEGER NOT NULL CHECK (costo_unitario >= 0),
    precio_venta   INTEGER NOT NULL CHECK (precio_venta >= 0),
    activo         INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0, 1)),
    creado_en      TEXT    NOT NULL,
    actualizado_en TEXT    NOT NULL,
    UNIQUE (empresa_id, codigo)
);

CREATE INDEX idx_producto_empresa ON producto (empresa_id, activo);
CREATE INDEX idx_producto_categoria ON producto (categoria_id);

-- Cada cambio de costo o precio queda registrado para analizar la evolución de márgenes.
CREATE TABLE historial_precio (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    producto_id    INTEGER NOT NULL REFERENCES producto (id) ON DELETE CASCADE,
    costo_unitario INTEGER NOT NULL,
    precio_venta   INTEGER NOT NULL,
    fecha          TEXT    NOT NULL,
    usuario        TEXT
);

CREATE INDEX idx_historial_precio_producto ON historial_precio (producto_id, fecha);
