-- V8: historial de informes generados (qué se generó, de qué período, quién y dónde se guardó).

CREATE TABLE informe_generado (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    empresa_id   INTEGER NOT NULL REFERENCES empresa (id),
    tipo         TEXT    NOT NULL,
    desde        TEXT    NOT NULL,
    hasta        TEXT    NOT NULL,
    archivo      TEXT    NOT NULL,
    generado_por TEXT    NOT NULL,
    generado_en  TEXT    NOT NULL
);

CREATE INDEX idx_informe_generado_empresa ON informe_generado (empresa_id, generado_en);
