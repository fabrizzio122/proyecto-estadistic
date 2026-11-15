-- V9: activos, deudas y pagos de deudas (balance general y patrimonio).
-- Montos en diezmilésimas (valor x 10000). La tasa de interés se guarda en porcentaje x 10000.

CREATE TABLE activo (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    empresa_id      INTEGER NOT NULL REFERENCES empresa (id),
    categoria       TEXT    NOT NULL,
    descripcion     TEXT    NOT NULL,
    -- Activos fijos: fecha de adquisición. Corrientes (caja, cuentas por cobrar): fecha del saldo.
    fecha           TEXT    NOT NULL,
    valor           INTEGER NOT NULL CHECK (valor >= 0),
    -- Años de vida útil para la depreciación en línea recta; NULL si no se deprecia.
    vida_util_anios INTEGER CHECK (vida_util_anios IS NULL OR vida_util_anios BETWEEN 1 AND 100),
    valor_residual  INTEGER NOT NULL DEFAULT 0 CHECK (valor_residual >= 0),
    estado          TEXT    NOT NULL DEFAULT 'VIGENTE' CHECK (estado IN ('VIGENTE', 'BAJA')),
    fecha_baja      TEXT,
    motivo_baja     TEXT,
    baja_por        TEXT,
    baja_en         TEXT,
    registrado_por  TEXT    NOT NULL,
    registrado_en   TEXT    NOT NULL,
    actualizado_por TEXT,
    actualizado_en  TEXT,
    CHECK (valor_residual <= valor)
);

CREATE INDEX idx_activo_empresa ON activo (empresa_id, estado);

CREATE TABLE deuda (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    empresa_id       INTEGER NOT NULL REFERENCES empresa (id),
    categoria        TEXT    NOT NULL,
    acreedor         TEXT    NOT NULL,
    descripcion      TEXT,
    fecha            TEXT    NOT NULL,
    monto            INTEGER NOT NULL CHECK (monto > 0),
    tasa_anual       INTEGER CHECK (tasa_anual IS NULL OR tasa_anual >= 0),
    cuota_mensual    INTEGER CHECK (cuota_mensual IS NULL OR cuota_mensual > 0),
    vencimiento      TEXT,
    estado           TEXT    NOT NULL DEFAULT 'VIGENTE' CHECK (estado IN ('VIGENTE', 'ANULADA')),
    motivo_anulacion TEXT,
    anulado_por      TEXT,
    anulado_en       TEXT,
    registrado_por   TEXT    NOT NULL,
    registrado_en    TEXT    NOT NULL,
    actualizado_por  TEXT,
    actualizado_en   TEXT
);

CREATE INDEX idx_deuda_empresa ON deuda (empresa_id, estado);

-- Pagos del capital de una deuda (los intereses se registran como gasto en Gestión financiera).
CREATE TABLE pago_deuda (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    deuda_id       INTEGER NOT NULL REFERENCES deuda (id),
    fecha          TEXT    NOT NULL,
    monto          INTEGER NOT NULL CHECK (monto > 0),
    nota           TEXT,
    estado         TEXT    NOT NULL DEFAULT 'REGISTRADO' CHECK (estado IN ('REGISTRADO', 'ANULADO')),
    registrado_por TEXT    NOT NULL,
    registrado_en  TEXT    NOT NULL,
    anulado_por    TEXT,
    anulado_en     TEXT
);

CREATE INDEX idx_pago_deuda ON pago_deuda (deuda_id, fecha);
