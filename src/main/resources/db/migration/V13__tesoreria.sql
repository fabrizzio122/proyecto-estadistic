-- V13: tesorería. El dinero de la empresa vive en cuentas (caja, bancos, billeteras digitales) y cada
-- registro que mueve dinero indica de qué cuenta sale o a cuál entra. El saldo de cada cuenta se calcula:
-- saldo inicial + entradas - salidas.

CREATE TABLE cuenta_dinero (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    empresa_id      INTEGER NOT NULL REFERENCES empresa (id),
    nombre          TEXT    NOT NULL COLLATE NOCASE,
    tipo            TEXT    NOT NULL CHECK (tipo IN ('CAJA', 'BANCO', 'BILLETERA')),
    -- Número de cuenta, CCI o celular de la billetera (solo referencia).
    numero          TEXT,
    -- Dinero que tenía la cuenta antes de cualquier movimiento registrado.
    saldo_inicial   INTEGER NOT NULL DEFAULT 0,
    activa          INTEGER NOT NULL DEFAULT 1 CHECK (activa IN (0, 1)),
    registrado_por  TEXT    NOT NULL,
    registrado_en   TEXT    NOT NULL,
    actualizado_por TEXT,
    actualizado_en  TEXT,
    UNIQUE (empresa_id, nombre)
);

-- Ventas y compras: al contado (el dinero entra o sale de una cuenta en la fecha de la operación) o al
-- crédito (queda por cobrar o por pagar hasta que se registran los cobros o pagos).
ALTER TABLE operacion ADD COLUMN condicion TEXT NOT NULL DEFAULT 'CONTADO'
    CHECK (condicion IN ('CONTADO', 'CREDITO'));
ALTER TABLE operacion ADD COLUMN cuenta_id INTEGER REFERENCES cuenta_dinero (id);
ALTER TABLE operacion ADD COLUMN vencimiento TEXT;

CREATE TABLE pago_operacion (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    operacion_id   INTEGER NOT NULL REFERENCES operacion (id),
    cuenta_id      INTEGER NOT NULL REFERENCES cuenta_dinero (id),
    fecha          TEXT    NOT NULL,
    monto          INTEGER NOT NULL CHECK (monto > 0),
    nota           TEXT,
    estado         TEXT    NOT NULL DEFAULT 'REGISTRADO' CHECK (estado IN ('REGISTRADO', 'ANULADO')),
    registrado_por TEXT    NOT NULL,
    registrado_en  TEXT    NOT NULL,
    anulado_por    TEXT,
    anulado_en     TEXT
);
CREATE INDEX idx_pago_operacion_operacion ON pago_operacion (operacion_id);

-- Ingresos y gastos: cuenta de la que sale o a la que entra el dinero.
ALTER TABLE movimiento_financiero ADD COLUMN cuenta_id INTEGER REFERENCES cuenta_dinero (id);

-- Activos: cuenta con la que se pagó la compra (vacío si no salió dinero de las cuentas).
ALTER TABLE activo ADD COLUMN cuenta_id INTEGER REFERENCES cuenta_dinero (id);

-- Deudas: cuenta a la que ingresó el dinero del préstamo (vacío si no ingresó dinero, p. ej. un proveedor).
ALTER TABLE deuda ADD COLUMN cuenta_id INTEGER REFERENCES cuenta_dinero (id);

-- Pagos de deudas: cuenta de la que salió el pago y la parte de intereses. El interés se registra también
-- como gasto financiero (movimiento_id) para que entre en el estado de resultados.
ALTER TABLE pago_deuda ADD COLUMN cuenta_id INTEGER REFERENCES cuenta_dinero (id);
ALTER TABLE pago_deuda ADD COLUMN interes INTEGER NOT NULL DEFAULT 0 CHECK (interes >= 0);
ALTER TABLE pago_deuda ADD COLUMN movimiento_id INTEGER REFERENCES movimiento_financiero (id);

-- Movimientos que solo mueven dinero (no son ingresos ni gastos): transferencias entre cuentas y
-- aportes o retiros del dueño.
CREATE TABLE movimiento_tesoreria (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    empresa_id        INTEGER NOT NULL REFERENCES empresa (id),
    tipo              TEXT    NOT NULL CHECK (tipo IN ('TRANSFERENCIA', 'APORTE', 'RETIRO')),
    fecha             TEXT    NOT NULL,
    cuenta_origen_id  INTEGER REFERENCES cuenta_dinero (id),
    cuenta_destino_id INTEGER REFERENCES cuenta_dinero (id),
    monto             INTEGER NOT NULL CHECK (monto > 0),
    descripcion       TEXT    NOT NULL,
    estado            TEXT    NOT NULL DEFAULT 'REGISTRADO' CHECK (estado IN ('REGISTRADO', 'ANULADO')),
    motivo_anulacion  TEXT,
    registrado_por    TEXT    NOT NULL,
    registrado_en     TEXT    NOT NULL,
    anulado_por       TEXT,
    anulado_en        TEXT,
    CHECK ((tipo = 'TRANSFERENCIA' AND cuenta_origen_id IS NOT NULL AND cuenta_destino_id IS NOT NULL
                AND cuenta_origen_id <> cuenta_destino_id)
        OR (tipo = 'APORTE' AND cuenta_origen_id IS NULL AND cuenta_destino_id IS NOT NULL)
        OR (tipo = 'RETIRO' AND cuenta_origen_id IS NOT NULL AND cuenta_destino_id IS NULL))
);
CREATE INDEX idx_movimiento_tesoreria_empresa ON movimiento_tesoreria (empresa_id, fecha);

-- Cada empresa empieza con una cuenta «Caja» cuyo saldo inicial es lo que tenía registrado como
-- «Caja y bancos». Los registros anteriores a esta versión no indican cuenta: se conservan en los
-- análisis, pero no mueven el saldo (el saldo inicial ya refleja el dinero de ese momento).
INSERT INTO cuenta_dinero (empresa_id, nombre, tipo, saldo_inicial, registrado_por, registrado_en)
SELECT e.id, 'Caja', 'CAJA',
       COALESCE((SELECT SUM(a.valor) FROM activo a
                 WHERE a.empresa_id = e.id AND a.categoria = 'CAJA_BANCOS' AND a.estado = 'VIGENTE'), 0),
       'sistema', strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime')
FROM empresa e;

-- «Caja y bancos» deja de ser un activo escrito a mano: su saldo pasó a la cuenta «Caja».
UPDATE activo
SET estado = 'BAJA',
    fecha_baja = date('now', 'localtime'),
    motivo_baja = 'Pasó a Tesorería: saldo inicial de la cuenta «Caja»',
    baja_por = 'sistema',
    baja_en = strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime')
WHERE categoria = 'CAJA_BANCOS' AND estado = 'VIGENTE';
