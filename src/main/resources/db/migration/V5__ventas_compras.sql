-- V5: ventas y compras (cabecera + líneas de detalle).
-- Montos y cantidades en diezmilésimas (valor x 10000), igual que en V4.
-- El stock de cada producto se calcula como compras - ventas de las operaciones REGISTRADAS.

CREATE TABLE operacion (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    empresa_id            INTEGER NOT NULL REFERENCES empresa (id),
    tipo                  TEXT    NOT NULL CHECK (tipo IN ('VENTA', 'COMPRA')),
    numero                TEXT    NOT NULL,
    fecha                 TEXT    NOT NULL,
    contraparte           TEXT,
    documento_contraparte TEXT,
    comprobante           TEXT,
    tasa_impuesto         INTEGER NOT NULL CHECK (tasa_impuesto BETWEEN 0 AND 1000000),
    subtotal              INTEGER NOT NULL,
    impuesto              INTEGER NOT NULL,
    total                 INTEGER NOT NULL,
    costo_total           INTEGER NOT NULL,
    estado                TEXT    NOT NULL DEFAULT 'REGISTRADA' CHECK (estado IN ('REGISTRADA', 'ANULADA')),
    observaciones         TEXT,
    registrado_por        TEXT    NOT NULL,
    registrado_en         TEXT    NOT NULL,
    motivo_anulacion      TEXT,
    anulado_por           TEXT,
    anulado_en            TEXT,
    UNIQUE (empresa_id, tipo, numero)
);

CREATE INDEX idx_operacion_empresa_fecha ON operacion (empresa_id, tipo, fecha);

CREATE TABLE operacion_linea (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    operacion_id    INTEGER NOT NULL REFERENCES operacion (id) ON DELETE CASCADE,
    producto_id     INTEGER NOT NULL REFERENCES producto (id),
    cantidad        INTEGER NOT NULL CHECK (cantidad > 0),
    precio_unitario INTEGER NOT NULL CHECK (precio_unitario >= 0),
    costo_unitario  INTEGER NOT NULL CHECK (costo_unitario >= 0),
    subtotal        INTEGER NOT NULL
);

CREATE INDEX idx_operacion_linea_operacion ON operacion_linea (operacion_id);
CREATE INDEX idx_operacion_linea_producto ON operacion_linea (producto_id);
