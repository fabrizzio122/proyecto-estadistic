-- V6: ingresos y gastos no comerciales (alquiler, sueldos, servicios, intereses...).
-- Las ventas y compras de mercadería están en la tabla operacion (V5).
-- Montos en diezmilésimas (valor x 10000), sin impuestos.

CREATE TABLE movimiento_financiero (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    empresa_id       INTEGER NOT NULL REFERENCES empresa (id),
    tipo             TEXT    NOT NULL CHECK (tipo IN ('INGRESO', 'GASTO')),
    categoria        TEXT    NOT NULL,
    comportamiento   TEXT    CHECK (comportamiento IN ('FIJO', 'VARIABLE')),
    fecha            TEXT    NOT NULL,
    descripcion      TEXT    NOT NULL,
    monto            INTEGER NOT NULL CHECK (monto > 0),
    comprobante      TEXT,
    estado           TEXT    NOT NULL DEFAULT 'REGISTRADO' CHECK (estado IN ('REGISTRADO', 'ANULADO')),
    registrado_por   TEXT    NOT NULL,
    registrado_en    TEXT    NOT NULL,
    actualizado_por  TEXT,
    actualizado_en   TEXT,
    motivo_anulacion TEXT,
    anulado_por      TEXT,
    anulado_en       TEXT,
    -- Los gastos deben indicar si son fijos o variables; los ingresos no.
    CHECK ((tipo = 'GASTO' AND comportamiento IS NOT NULL) OR (tipo = 'INGRESO' AND comportamiento IS NULL))
);

CREATE INDEX idx_movimiento_empresa_fecha ON movimiento_financiero (empresa_id, fecha);
