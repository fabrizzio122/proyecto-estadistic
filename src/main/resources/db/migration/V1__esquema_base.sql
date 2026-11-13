-- V1: esquema base de Business Analyzer.
-- Las tablas de negocio (usuarios, empresas, productos, ventas...) se agregan
-- en migraciones nuevas (V2, V3...) durante las fases siguientes.

CREATE TABLE configuracion (
    clave          TEXT PRIMARY KEY CHECK (length(clave) BETWEEN 1 AND 100),
    valor          TEXT NOT NULL,
    actualizado_en TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime'))
);

INSERT INTO configuracion (clave, valor) VALUES ('app.tema', 'oscuro');
