-- V10: ganadería (vacunos, ovinos, alpacas y llamas) para empresas del sector agropecuario.
-- Las existencias no se guardan: se calculan con los movimientos (inventario inicial, nacimientos,
-- compras, ventas, muertes...). Montos en diezmilésimas (valor x 10000).

INSERT INTO permiso (codigo, descripcion) VALUES
    ('AGRO_VER', 'Ver ganadería y agricultura'),
    ('AGRO_GESTIONAR', 'Registrar ganado, producción y campañas agrícolas');

-- Administrador y gerente: ver y registrar. Analista: ver. Operador (el productor): ver y registrar.
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
    -- Solo en los cambios de categoría (por ejemplo, de ternera a vaquillona).
    categoria_destino TEXT,
    -- Valor total de la compra o la venta (opcional).
    monto             INTEGER CHECK (monto IS NULL OR monto >= 0),
    -- Causa de la muerte o de la pérdida (opcional).
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
    -- Días que abarca el registro: 1 si es la producción de un día, 30 si es el total del mes.
    dias             INTEGER NOT NULL DEFAULT 1 CHECK (dias BETWEEN 1 AND 366),
    cantidad         INTEGER NOT NULL CHECK (cantidad > 0),
    -- Vacas en ordeño o animales esquilados (opcional, para los promedios).
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

-- Precio de mercado por cabeza de cada categoría, para valorizar el hato.
CREATE TABLE precio_ganado (
    empresa_id      INTEGER NOT NULL REFERENCES empresa (id),
    categoria       TEXT    NOT NULL,
    precio          INTEGER NOT NULL CHECK (precio >= 0),
    actualizado_por TEXT    NOT NULL,
    actualizado_en  TEXT    NOT NULL,
    PRIMARY KEY (empresa_id, categoria)
);
