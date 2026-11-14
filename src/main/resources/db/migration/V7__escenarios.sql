-- V7: escenarios de simulación guardados por empresa.
-- Los cambios porcentuales se guardan como fracción x 10000 (0.05 = 5 % -> 500);
-- el costo fijo adicional como monto x 10000.

CREATE TABLE escenario (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    empresa_id           INTEGER NOT NULL REFERENCES empresa (id),
    nombre               TEXT    NOT NULL COLLATE NOCASE,
    descripcion          TEXT,
    cambio_precio        INTEGER NOT NULL,
    cambio_volumen       INTEGER NOT NULL,
    cambio_costo_unitario INTEGER NOT NULL,
    cambio_gastos_variables INTEGER NOT NULL,
    cambio_costos_fijos  INTEGER NOT NULL,
    costo_fijo_adicional INTEGER NOT NULL,
    tasa_impuesto        INTEGER NOT NULL,
    creado_por           TEXT    NOT NULL,
    actualizado_en       TEXT    NOT NULL,
    UNIQUE (empresa_id, nombre)
);
