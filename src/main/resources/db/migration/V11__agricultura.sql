CREATE TABLE parcela (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    empresa_id      INTEGER NOT NULL REFERENCES empresa (id),
    nombre          TEXT    NOT NULL,
    area            INTEGER NOT NULL CHECK (area > 0),
    tenencia        TEXT    NOT NULL,
    riego           INTEGER NOT NULL DEFAULT 0 CHECK (riego IN (0, 1)),
    ubicacion       TEXT,
    activa          INTEGER NOT NULL DEFAULT 1 CHECK (activa IN (0, 1)),
    registrado_por  TEXT    NOT NULL,
    registrado_en   TEXT    NOT NULL,
    actualizado_por TEXT,
    actualizado_en  TEXT
);

CREATE UNIQUE INDEX ux_parcela_nombre ON parcela (empresa_id, nombre COLLATE NOCASE);

CREATE TABLE campania (
    id                       INTEGER PRIMARY KEY AUTOINCREMENT,
    empresa_id               INTEGER NOT NULL REFERENCES empresa (id),
    parcela_id               INTEGER NOT NULL REFERENCES parcela (id),
    cultivo                  TEXT    NOT NULL,
    variedad                 TEXT,
    fecha_siembra            TEXT    NOT NULL,
    area                     INTEGER NOT NULL CHECK (area > 0),
    rendimiento_referencia   INTEGER CHECK (rendimiento_referencia IS NULL OR rendimiento_referencia > 0),
    precio_chacra            INTEGER CHECK (precio_chacra IS NULL OR precio_chacra >= 0),
    estado                   TEXT    NOT NULL DEFAULT 'EN_CURSO'
                                     CHECK (estado IN ('EN_CURSO', 'COSECHADA', 'PERDIDA', 'ANULADA')),
    fecha_cierre             TEXT,
    motivo                   TEXT,
    observacion              TEXT,
    registrado_por           TEXT    NOT NULL,
    registrado_en            TEXT    NOT NULL,
    actualizado_por          TEXT,
    actualizado_en           TEXT
);

CREATE INDEX idx_campania_empresa ON campania (empresa_id, fecha_siembra);

CREATE TABLE costo_campania (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    campania_id    INTEGER NOT NULL REFERENCES campania (id),
    fecha          TEXT    NOT NULL,
    rubro          TEXT    NOT NULL,
    descripcion    TEXT    NOT NULL,
    monto          INTEGER NOT NULL CHECK (monto > 0),
    estado         TEXT    NOT NULL DEFAULT 'REGISTRADO' CHECK (estado IN ('REGISTRADO', 'ANULADO')),
    registrado_por TEXT    NOT NULL,
    registrado_en  TEXT    NOT NULL,
    anulado_por    TEXT,
    anulado_en     TEXT
);

CREATE INDEX idx_costo_campania ON costo_campania (campania_id);

CREATE TABLE cosecha (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    campania_id    INTEGER NOT NULL REFERENCES campania (id),
    fecha          TEXT    NOT NULL,
    cantidad       INTEGER NOT NULL CHECK (cantidad > 0),
    destino        TEXT    NOT NULL,
    precio         INTEGER CHECK (precio IS NULL OR precio >= 0),
    observacion    TEXT,
    estado         TEXT    NOT NULL DEFAULT 'REGISTRADO' CHECK (estado IN ('REGISTRADO', 'ANULADO')),
    registrado_por TEXT    NOT NULL,
    registrado_en  TEXT    NOT NULL,
    anulado_por    TEXT,
    anulado_en     TEXT
);

CREATE INDEX idx_cosecha_campania ON cosecha (campania_id);
