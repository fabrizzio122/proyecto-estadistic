INSERT INTO permiso (codigo, descripcion) VALUES
    ('AGRO_VER', 'Ver ganadería y agricultura'),
    ('AGRO_GESTIONAR', 'Registrar ganado, producción y campañas agrícolas');

INSERT INTO rol_permiso (rol_id, permiso_codigo) VALUES
    (1, 'AGRO_VER'), (1, 'AGRO_GESTIONAR'),
    (2, 'AGRO_VER'), (2, 'AGRO_GESTIONAR'),
    (3, 'AGRO_VER'),
    (4, 'AGRO_VER'), (4, 'AGRO_GESTIONAR');

CREATE TABLE movimiento_ganado (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    empresa_id        INTEGER NOT NULL REFERENCES empresa (id),
    fecha             TEXT    NOT NULL,
    tipo              TEXT    NOT NULL,
    categoria         TEXT    NOT NULL,
    cantidad          INTEGER NOT NULL CHECK (cantidad > 0),
    categoria_destino TEXT,
    monto             INTEGER CHECK (monto IS NULL OR monto >= 0),
    causa             TEXT,
    observacion       TEXT,
    estado            TEXT    NOT NULL DEFAULT 'REGISTRADO' CHECK (estado IN ('REGISTRADO', 'ANULADO')),
    motivo_anulacion  TEXT,
    anulado_por       TEXT,
    anulado_en        TEXT,
    registrado_por    TEXT    NOT NULL,
    registrado_en     TEXT    NOT NULL,
    CHECK ((tipo = 'CAMBIO_CATEGORIA') = (categoria_destino IS NOT NULL))
);

CREATE INDEX idx_movimiento_ganado ON movimiento_ganado (empresa_id, fecha);

CREATE TABLE produccion_ganadera (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    empresa_id       INTEGER NOT NULL REFERENCES empresa (id),
    especie          TEXT    NOT NULL,
    producto         TEXT    NOT NULL,
    fecha            TEXT    NOT NULL,
    dias             INTEGER NOT NULL DEFAULT 1 CHECK (dias BETWEEN 1 AND 366),
    cantidad         INTEGER NOT NULL CHECK (cantidad > 0),
    animales         INTEGER CHECK (animales IS NULL OR animales > 0),
    precio_unitario  INTEGER CHECK (precio_unitario IS NULL OR precio_unitario >= 0),
    observacion      TEXT,
    estado           TEXT    NOT NULL DEFAULT 'REGISTRADO' CHECK (estado IN ('REGISTRADO', 'ANULADO')),
    anulado_por      TEXT,
    anulado_en       TEXT,
    registrado_por   TEXT    NOT NULL,
    registrado_en    TEXT    NOT NULL
);

CREATE INDEX idx_produccion_ganadera ON produccion_ganadera (empresa_id, fecha);

CREATE TABLE precio_ganado (
    empresa_id      INTEGER NOT NULL REFERENCES empresa (id),
    categoria       TEXT    NOT NULL,
    precio          INTEGER NOT NULL CHECK (precio >= 0),
    actualizado_por TEXT    NOT NULL,
    actualizado_en  TEXT    NOT NULL,
    PRIMARY KEY (empresa_id, categoria)
);
