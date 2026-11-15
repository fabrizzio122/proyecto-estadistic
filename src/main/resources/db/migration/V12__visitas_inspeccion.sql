-- V12: visitas de inspección o supervisión a productores agropecuarios (verificación en campo,
-- compromisos y fotos) para el informe técnico.

INSERT INTO permiso (codigo, descripcion) VALUES
    ('VISITAS_REGISTRAR', 'Registrar visitas de inspección y verificar compromisos');

-- Administrador, gerente y analista (el inspector o supervisor que revisa al productor).
INSERT INTO rol_permiso (rol_id, permiso_codigo) VALUES
    (1, 'VISITAS_REGISTRAR'), (2, 'VISITAS_REGISTRAR'), (3, 'VISITAS_REGISTRAR');

CREATE TABLE visita (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    empresa_id       INTEGER NOT NULL REFERENCES empresa (id),
    fecha            TEXT    NOT NULL,
    tipo             TEXT    NOT NULL,
    inspector        TEXT    NOT NULL,
    lugar            TEXT,
    -- Coordenadas en grados decimales (WGS84), opcionales.
    latitud          REAL CHECK (latitud IS NULL OR latitud BETWEEN -90 AND 90),
    longitud         REAL CHECK (longitud IS NULL OR longitud BETWEEN -180 AND 180),
    atendido_por     TEXT,
    observaciones    TEXT,
    conclusion       TEXT    NOT NULL,
    proxima_visita   TEXT,
    estado           TEXT    NOT NULL DEFAULT 'REGISTRADA' CHECK (estado IN ('REGISTRADA', 'ANULADA')),
    motivo_anulacion TEXT,
    registrado_por   TEXT    NOT NULL,
    registrado_en    TEXT    NOT NULL,
    actualizado_por  TEXT,
    actualizado_en   TEXT
);

CREATE INDEX idx_visita_empresa ON visita (empresa_id, fecha);

-- Resultado de cada aspecto verificado en la visita.
CREATE TABLE visita_aspecto (
    visita_id   INTEGER NOT NULL REFERENCES visita (id),
    aspecto     TEXT    NOT NULL,
    resultado   TEXT    NOT NULL,
    observacion TEXT,
    PRIMARY KEY (visita_id, aspecto)
);

-- Compromisos o recomendaciones acordados en una visita; se verifican en una visita posterior.
CREATE TABLE compromiso (
    id                     INTEGER PRIMARY KEY AUTOINCREMENT,
    empresa_id             INTEGER NOT NULL REFERENCES empresa (id),
    visita_id              INTEGER NOT NULL REFERENCES visita (id),
    descripcion            TEXT    NOT NULL,
    plazo                  TEXT,
    estado                 TEXT    NOT NULL DEFAULT 'PENDIENTE'
                                   CHECK (estado IN ('PENDIENTE', 'CUMPLIDO', 'INCUMPLIDO')),
    fecha_verificacion     TEXT,
    visita_verificacion_id INTEGER REFERENCES visita (id),
    observacion            TEXT,
    verificado_por         TEXT
);

CREATE INDEX idx_compromiso_empresa ON compromiso (empresa_id, estado);

-- Fotos de la visita. El archivo se copia a la carpeta de datos (fotos-visitas/<visita>/).
CREATE TABLE visita_foto (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    visita_id     INTEGER NOT NULL REFERENCES visita (id),
    archivo       TEXT    NOT NULL,
    descripcion   TEXT,
    registrado_en TEXT    NOT NULL
);
