package com.businessanalyzer.repository;

import com.businessanalyzer.analytics.CalculadoraOperacionFGEM;
import com.businessanalyzer.analytics.RentabilidadProductoFGEM;
import com.businessanalyzer.model.TipoItemFGEM;
import java.time.YearMonth;
import java.util.Map;
import java.util.TreeMap;
import com.businessanalyzer.analytics.ResumenOperacionesFGEM;
import com.businessanalyzer.exception.DataAccessExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CondicionPagoFGEM;
import com.businessanalyzer.model.EstadoOperacionFGEM;
import com.businessanalyzer.model.LineaOperacionFGEM;
import com.businessanalyzer.model.OperacionFGEM;
import com.businessanalyzer.model.TipoOperacionFGEM;
import com.businessanalyzer.model.UnidadMedidaFGEM;
import com.businessanalyzer.util.FechasFGEM;
import com.businessanalyzer.util.MontosFGEM;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Acceso a las tablas {@code operacion} y {@code operacion_linea}. */
public class OperacionRepositoryFGEM {

    private final VentasAgroRepositoryFGEM agroFGEM = new VentasAgroRepositoryFGEM();

    /** Cabecera de una operación nueva, con los totales ya calculados. */
    public record NuevaOperacionFGEM(TipoOperacionFGEM tipoFGEM, String numeroFGEM, LocalDate fechaFGEM, String contraparteFGEM,
                                 String documentoContraparteFGEM, String comprobanteFGEM, BigDecimal tasaImpuestoFGEM,
                                 CalculadoraOperacionFGEM.TotalesFGEM totalesFGEM, String observacionesFGEM,
                                 String registradoPorFGEM, LocalDateTime registradoEnFGEM, CondicionPagoFGEM condicionFGEM,
                                 Long cuentaIdFGEM, LocalDate vencimientoFGEM) {
    }

    private static final String CABECERA_FGEM = """
            SELECT o.id, o.empresa_id, o.tipo, o.numero, o.fecha, o.contraparte, o.documento_contraparte,
                   o.comprobante, o.tasa_impuesto, o.subtotal, o.impuesto, o.total, o.costo_total, o.estado,
                   o.observaciones, o.registrado_por, o.registrado_en, o.motivo_anulacion, o.anulado_por,
                   o.anulado_en, o.condicion, o.cuenta_id, cd.nombre AS cuenta, o.vencimiento,
                   (SELECT COUNT(*) FROM operacion_linea ol WHERE ol.operacion_id = o.id) AS items,
                   (SELECT COALESCE(SUM(po.monto), 0) FROM pago_operacion po
                    WHERE po.operacion_id = o.id AND po.estado = 'REGISTRADO') AS pagado
            FROM operacion o
            LEFT JOIN cuenta_dinero cd ON cd.id = o.cuenta_id""";

    public List<OperacionFGEM> listarFGEM(Connection cFGEM, long empresaIdFGEM, TipoOperacionFGEM tipoFGEM, LocalDate desdeFGEM,
                                  LocalDate hastaFGEM, boolean incluirAnuladasFGEM) throws SQLException {
        String sqlFGEM = CABECERA_FGEM + " WHERE o.empresa_id = ? AND o.tipo = ? AND o.fecha BETWEEN ? AND ?"
                + (incluirAnuladasFGEM ? "" : " AND o.estado = 'REGISTRADA'")
                + " ORDER BY o.fecha DESC, o.id DESC";
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(sqlFGEM)) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setString(2, tipoFGEM.name());
            psFGEM.setString(3, FechasFGEM.fechaATextoFGEM(desdeFGEM));
            psFGEM.setString(4, FechasFGEM.fechaATextoFGEM(hastaFGEM));
            return mapearCabecerasFGEM(psFGEM);
        }
    }

    /** Cabecera y líneas. */
    public Optional<OperacionFGEM> buscarPorIdFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(CABECERA_FGEM + " WHERE o.id = ?")) {
            psFGEM.setLong(1, idFGEM);
            List<OperacionFGEM> resultadoFGEM = mapearCabecerasFGEM(psFGEM);
            if (resultadoFGEM.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(resultadoFGEM.get(0).conLineasFGEM(lineasFGEM(cFGEM, idFGEM)));
        }
    }

    /**
     * Ventas o compras al crédito registradas que aún tienen saldo por cobrar o pagar, de la más
     * antigua a la más reciente.
     */
    public List<OperacionFGEM> listarPendientesFGEM(Connection cFGEM, long empresaIdFGEM, TipoOperacionFGEM tipoFGEM) throws SQLException {
        String sqlFGEM = CABECERA_FGEM + " WHERE o.empresa_id = ? AND o.tipo = ? AND o.estado = 'REGISTRADA'"
                + " AND o.condicion = 'CREDITO' ORDER BY COALESCE(o.vencimiento, o.fecha), o.fecha, o.id";
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(sqlFGEM)) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setString(2, tipoFGEM.name());
            return mapearCabecerasFGEM(psFGEM).stream().filter(oFGEM -> oFGEM.pendienteFGEM().signum() > 0).toList();
        }
    }

    public List<LineaOperacionFGEM> lineasFGEM(Connection cFGEM, long operacionIdFGEM) throws SQLException {
        List<LineaOperacionFGEM> lineasFGEM = new ArrayList<>();
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                SELECT ol.id, ol.producto_id, p.codigo, p.nombre, p.unidad, ol.cantidad, ol.precio_unitario,
                       ol.costo_unitario, ol.subtotal
                FROM operacion_linea ol
                JOIN producto p ON p.id = ol.producto_id
                WHERE ol.operacion_id = ?
                ORDER BY ol.id""")) {
            psFGEM.setLong(1, operacionIdFGEM);
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                while (rsFGEM.next()) {
                    lineasFGEM.add(new LineaOperacionFGEM(
                            rsFGEM.getLong("id"),
                            rsFGEM.getLong("producto_id"),
                            rsFGEM.getString("codigo"),
                            rsFGEM.getString("nombre"),
                            UnidadMedidaFGEM.valueOf(rsFGEM.getString("unidad")),
                            MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("cantidad")),
                            MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("precio_unitario")),
                            MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("costo_unitario")),
                            MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("subtotal"))));
                }
            }
        }
        return lineasFGEM;
    }

    /** Siguiente número para la empresa y el tipo: V-000001, C-000001... */
    public String siguienteNumeroFGEM(Connection cFGEM, long empresaIdFGEM, TipoOperacionFGEM tipoFGEM) throws SQLException {
        String prefijoFGEM = tipoFGEM.prefijoNumeroFGEM();
        int mayorFGEM = 0;
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "SELECT numero FROM operacion WHERE empresa_id = ? AND tipo = ?")) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setString(2, tipoFGEM.name());
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                while (rsFGEM.next()) {
                    String numeroFGEM = rsFGEM.getString(1);
                    if (numeroFGEM.startsWith(prefijoFGEM + "-") && numeroFGEM.substring(prefijoFGEM.length() + 1).matches("\\d{1,9}")) {
                        mayorFGEM = Math.max(mayorFGEM, Integer.parseInt(numeroFGEM.substring(prefijoFGEM.length() + 1)));
                    }
                }
            }
        }
        return String.format("%s-%06d", prefijoFGEM, mayorFGEM + 1);
    }

    public long insertarFGEM(Connection cFGEM, long empresaIdFGEM, NuevaOperacionFGEM nFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                INSERT INTO operacion (empresa_id, tipo, numero, fecha, contraparte, documento_contraparte,
                                       comprobante, tasa_impuesto, subtotal, impuesto, total, costo_total,
                                       observaciones, registrado_por, registrado_en, condicion, cuenta_id,
                                       vencimiento)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setString(2, nFGEM.tipoFGEM().name());
            psFGEM.setString(3, nFGEM.numeroFGEM());
            psFGEM.setString(4, FechasFGEM.fechaATextoFGEM(nFGEM.fechaFGEM()));
            psFGEM.setString(5, nFGEM.contraparteFGEM());
            psFGEM.setString(6, nFGEM.documentoContraparteFGEM());
            psFGEM.setString(7, nFGEM.comprobanteFGEM());
            psFGEM.setLong(8, MontosFGEM.aEnteroFGEM(nFGEM.tasaImpuestoFGEM()));
            psFGEM.setLong(9, MontosFGEM.aEnteroFGEM(nFGEM.totalesFGEM().subtotalFGEM()));
            psFGEM.setLong(10, MontosFGEM.aEnteroFGEM(nFGEM.totalesFGEM().impuestoFGEM()));
            psFGEM.setLong(11, MontosFGEM.aEnteroFGEM(nFGEM.totalesFGEM().totalFGEM()));
            psFGEM.setLong(12, MontosFGEM.aEnteroFGEM(nFGEM.totalesFGEM().costoTotalFGEM()));
            psFGEM.setString(13, nFGEM.observacionesFGEM());
            psFGEM.setString(14, nFGEM.registradoPorFGEM());
            psFGEM.setString(15, FechasFGEM.aTextoFGEM(nFGEM.registradoEnFGEM()));
            psFGEM.setString(16, (nFGEM.condicionFGEM() == null ? CondicionPagoFGEM.CONTADO : nFGEM.condicionFGEM()).name());
            if (nFGEM.cuentaIdFGEM() == null) {
                psFGEM.setNull(17, java.sql.Types.INTEGER);
            } else {
                psFGEM.setLong(17, nFGEM.cuentaIdFGEM());
            }
            psFGEM.setString(18, FechasFGEM.fechaATextoFGEM(nFGEM.vencimientoFGEM()));
            psFGEM.executeUpdate();
            try (ResultSet clavesFGEM = psFGEM.getGeneratedKeys()) {
                if (!clavesFGEM.next()) {
                    throw new DataAccessExceptionFGEM("No se obtuvo el identificador de la operación creada");
                }
                return clavesFGEM.getLong(1);
            }
        }
    }

    public void insertarLineaFGEM(Connection cFGEM, long operacionIdFGEM, long productoIdFGEM, BigDecimal cantidadFGEM,
                              BigDecimal precioUnitarioFGEM, BigDecimal costoUnitarioFGEM, BigDecimal subtotalFGEM)
            throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                INSERT INTO operacion_linea (operacion_id, producto_id, cantidad, precio_unitario,
                                             costo_unitario, subtotal)
                VALUES (?, ?, ?, ?, ?, ?)""")) {
            psFGEM.setLong(1, operacionIdFGEM);
            psFGEM.setLong(2, productoIdFGEM);
            psFGEM.setLong(3, MontosFGEM.aEnteroFGEM(cantidadFGEM));
            psFGEM.setLong(4, MontosFGEM.aEnteroFGEM(precioUnitarioFGEM));
            psFGEM.setLong(5, MontosFGEM.aEnteroFGEM(costoUnitarioFGEM));
            psFGEM.setLong(6, MontosFGEM.aEnteroFGEM(subtotalFGEM));
            psFGEM.executeUpdate();
        }
    }

    public void anularFGEM(Connection cFGEM, long idFGEM, String motivoFGEM, String usuarioFGEM, LocalDateTime fechaFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                UPDATE operacion
                SET estado = 'ANULADA', motivo_anulacion = ?, anulado_por = ?, anulado_en = ?
                WHERE id = ? AND estado = 'REGISTRADA'""")) {
            psFGEM.setString(1, motivoFGEM);
            psFGEM.setString(2, usuarioFGEM);
            psFGEM.setString(3, FechasFGEM.aTextoFGEM(fechaFGEM));
            psFGEM.setLong(4, idFGEM);
            if (psFGEM.executeUpdate() != 1) {
                throw new ValidacionExceptionFGEM("La operación ya no existe o ya estaba anulada. Actualiza la lista.");
            }
        }
    }

    /**
     * Totales del período para los análisis: operaciones registradas más las ventas y costos de la
     * producción agropecuaria (ver {@link VentasAgroRepositoryFGEM}).
     */
    public ResumenOperacionesFGEM resumenFGEM(Connection cFGEM, long empresaIdFGEM, LocalDate desdeFGEM, LocalDate hastaFGEM)
            throws SQLException {
        ResumenOperacionesFGEM opsFGEM = resumenOperacionesFGEM(cFGEM, empresaIdFGEM, desdeFGEM, hastaFGEM);
        BigDecimal ventasFGEM = opsFGEM.ventasNetasFGEM();
        BigDecimal costoFGEM = opsFGEM.costoVentasFGEM();
        int cantidadFGEM = opsFGEM.cantidadVentasFGEM();
        for (VentasAgroRepositoryFGEM.PartidaFGEM pFGEM : agroFGEM.partidasFGEM(cFGEM, empresaIdFGEM, desdeFGEM, hastaFGEM)) {
            ventasFGEM = ventasFGEM.add(pFGEM.ventaFGEM());
            costoFGEM = costoFGEM.add(pFGEM.costoFGEM());
            cantidadFGEM += pFGEM.esVentaFGEM() ? 1 : 0;
        }
        return new ResumenOperacionesFGEM(ventasFGEM, costoFGEM, opsFGEM.comprasNetasFGEM(), cantidadFGEM, opsFGEM.cantidadComprasFGEM());
    }

    /** Totales del período, solo con las operaciones de «Ventas y compras» registradas (no anuladas). */
    public ResumenOperacionesFGEM resumenOperacionesFGEM(Connection cFGEM, long empresaIdFGEM, LocalDate desdeFGEM, LocalDate hastaFGEM)
            throws SQLException {
        BigDecimal ventasFGEM = BigDecimal.ZERO;
        BigDecimal costoVentasFGEM = BigDecimal.ZERO;
        BigDecimal comprasFGEM = BigDecimal.ZERO;
        int cantidadVentasFGEM = 0;
        int cantidadComprasFGEM = 0;
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                SELECT tipo, COUNT(*) AS cantidad, COALESCE(SUM(subtotal), 0) AS subtotal,
                       COALESCE(SUM(costo_total), 0) AS costo
                FROM operacion
                WHERE empresa_id = ? AND estado = 'REGISTRADA' AND fecha BETWEEN ? AND ?
                GROUP BY tipo""")) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setString(2, FechasFGEM.fechaATextoFGEM(desdeFGEM));
            psFGEM.setString(3, FechasFGEM.fechaATextoFGEM(hastaFGEM));
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                while (rsFGEM.next()) {
                    if (TipoOperacionFGEM.VENTA.name().equals(rsFGEM.getString("tipo"))) {
                        cantidadVentasFGEM = rsFGEM.getInt("cantidad");
                        ventasFGEM = MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("subtotal"));
                        costoVentasFGEM = MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("costo"));
                    } else {
                        cantidadComprasFGEM = rsFGEM.getInt("cantidad");
                        comprasFGEM = MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("subtotal"));
                    }
                }
            }
        }
        return new ResumenOperacionesFGEM(ventasFGEM, costoVentasFGEM, comprasFGEM, cantidadVentasFGEM, cantidadComprasFGEM);
    }

    /** Totales de cada mes del rango (solo registradas), para el gráfico de evolución. */
    public Map<YearMonth, ResumenOperacionesFGEM> resumenMensualFGEM(Connection cFGEM, long empresaIdFGEM, LocalDate desdeFGEM,
                                                            LocalDate hastaFGEM) throws SQLException {
        Map<YearMonth, ResumenOperacionesFGEM> porMesFGEM = new TreeMap<>();
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                SELECT substr(fecha, 1, 7) AS mes, tipo, COUNT(*) AS cantidad,
                       COALESCE(SUM(subtotal), 0) AS subtotal, COALESCE(SUM(costo_total), 0) AS costo
                FROM operacion
                WHERE empresa_id = ? AND estado = 'REGISTRADA' AND fecha BETWEEN ? AND ?
                GROUP BY mes, tipo""")) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setString(2, FechasFGEM.fechaATextoFGEM(desdeFGEM));
            psFGEM.setString(3, FechasFGEM.fechaATextoFGEM(hastaFGEM));
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                while (rsFGEM.next()) {
                    YearMonth mesFGEM = YearMonth.parse(rsFGEM.getString("mes"));
                    ResumenOperacionesFGEM actualFGEM = porMesFGEM.getOrDefault(mesFGEM, ResumenOperacionesFGEM.VACIO_FGEM);
                    BigDecimal subtotalFGEM = MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("subtotal"));
                    int cantidadFGEM = rsFGEM.getInt("cantidad");
                    if (TipoOperacionFGEM.VENTA.name().equals(rsFGEM.getString("tipo"))) {
                        actualFGEM = new ResumenOperacionesFGEM(subtotalFGEM, MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("costo")),
                                actualFGEM.comprasNetasFGEM(), cantidadFGEM, actualFGEM.cantidadComprasFGEM());
                    } else {
                        actualFGEM = new ResumenOperacionesFGEM(actualFGEM.ventasNetasFGEM(), actualFGEM.costoVentasFGEM(), subtotalFGEM,
                                actualFGEM.cantidadVentasFGEM(), cantidadFGEM);
                    }
                    porMesFGEM.put(mesFGEM, actualFGEM);
                }
            }
        }
        for (VentasAgroRepositoryFGEM.PartidaFGEM pFGEM : agroFGEM.partidasFGEM(cFGEM, empresaIdFGEM, desdeFGEM, hastaFGEM)) {
            YearMonth mesFGEM = YearMonth.from(pFGEM.fechaFGEM());
            ResumenOperacionesFGEM actualFGEM = porMesFGEM.getOrDefault(mesFGEM, ResumenOperacionesFGEM.VACIO_FGEM);
            porMesFGEM.put(mesFGEM, new ResumenOperacionesFGEM(actualFGEM.ventasNetasFGEM().add(pFGEM.ventaFGEM()), actualFGEM.costoVentasFGEM().add(pFGEM.costoFGEM()),
                    actualFGEM.comprasNetasFGEM(), actualFGEM.cantidadVentasFGEM() + (pFGEM.esVentaFGEM() ? 1 : 0), actualFGEM.cantidadComprasFGEM()));
        }
        return porMesFGEM;
    }

    /** Fecha de la última venta registrada de cada producto en el período (producto → fecha). */
    public Map<Long, LocalDate> ultimasVentasFGEM(Connection cFGEM, long empresaIdFGEM, LocalDate desdeFGEM, LocalDate hastaFGEM) throws SQLException {
        Map<Long, LocalDate> ultimasFGEM = new java.util.HashMap<>();
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                SELECT ol.producto_id, MAX(o.fecha) AS ultima
                FROM operacion_linea ol
                JOIN operacion o ON o.id = ol.operacion_id
                WHERE o.empresa_id = ? AND o.tipo = 'VENTA' AND o.estado = 'REGISTRADA'
                  AND o.fecha BETWEEN ? AND ?
                GROUP BY ol.producto_id""")) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setString(2, FechasFGEM.fechaATextoFGEM(desdeFGEM));
            psFGEM.setString(3, FechasFGEM.fechaATextoFGEM(hastaFGEM));
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                while (rsFGEM.next()) {
                    ultimasFGEM.put(rsFGEM.getLong("producto_id"), FechasFGEM.fechaDesdeTextoFGEM(rsFGEM.getString("ultima")));
                }
            }
        }
        return ultimasFGEM;
    }

    /** Líneas de las ventas registradas del período, para la rentabilidad por producto. */
    public List<RentabilidadProductoFGEM.VentaFGEM> lineasVendidasFGEM(Connection cFGEM, long empresaIdFGEM, LocalDate desdeFGEM,
                                                           LocalDate hastaFGEM) throws SQLException {
        List<RentabilidadProductoFGEM.VentaFGEM> lineasFGEM = new ArrayList<>();
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                SELECT p.id, p.codigo, p.nombre, p.tipo, cat.nombre AS categoria,
                       ol.cantidad, ol.subtotal, ol.costo_unitario
                FROM operacion_linea ol
                JOIN operacion o ON o.id = ol.operacion_id
                JOIN producto p ON p.id = ol.producto_id
                LEFT JOIN categoria cat ON cat.id = p.categoria_id
                WHERE o.empresa_id = ? AND o.tipo = 'VENTA' AND o.estado = 'REGISTRADA'
                  AND o.fecha BETWEEN ? AND ?""")) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setString(2, FechasFGEM.fechaATextoFGEM(desdeFGEM));
            psFGEM.setString(3, FechasFGEM.fechaATextoFGEM(hastaFGEM));
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                while (rsFGEM.next()) {
                    lineasFGEM.add(new RentabilidadProductoFGEM.VentaFGEM(
                            rsFGEM.getLong("id"),
                            rsFGEM.getString("codigo"),
                            rsFGEM.getString("nombre"),
                            TipoItemFGEM.valueOf(rsFGEM.getString("tipo")),
                            rsFGEM.getString("categoria"),
                            MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("cantidad")),
                            MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("subtotal")),
                            MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("costo_unitario"))));
                }
            }
        }
        for (VentasAgroRepositoryFGEM.PartidaFGEM pFGEM : agroFGEM.partidasFGEM(cFGEM, empresaIdFGEM, desdeFGEM, hastaFGEM)) {
            if (pFGEM.esVentaFGEM() && pFGEM.cantidadFGEM().signum() > 0) {
                lineasFGEM.add(new RentabilidadProductoFGEM.VentaFGEM(pFGEM.claveFGEM(), "AGRO", pFGEM.nombreFGEM(), TipoItemFGEM.PRODUCTO,
                        "Producción agropecuaria", pFGEM.cantidadFGEM(), pFGEM.ventaFGEM(),
                        pFGEM.costoFGEM().divide(pFGEM.cantidadFGEM(), MontosFGEM.ESCALA_FGEM, java.math.RoundingMode.HALF_UP)));
            }
        }
        return lineasFGEM;
    }

    private static List<OperacionFGEM> mapearCabecerasFGEM(PreparedStatement psFGEM) throws SQLException {
        List<OperacionFGEM> operacionesFGEM = new ArrayList<>();
        try (ResultSet rsFGEM = psFGEM.executeQuery()) {
            while (rsFGEM.next()) {
                operacionesFGEM.add(new OperacionFGEM(
                        rsFGEM.getLong("id"),
                        rsFGEM.getLong("empresa_id"),
                        TipoOperacionFGEM.valueOf(rsFGEM.getString("tipo")),
                        rsFGEM.getString("numero"),
                        FechasFGEM.fechaDesdeTextoFGEM(rsFGEM.getString("fecha")),
                        rsFGEM.getString("contraparte"),
                        rsFGEM.getString("documento_contraparte"),
                        rsFGEM.getString("comprobante"),
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("tasa_impuesto")),
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("subtotal")),
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("impuesto")),
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("total")),
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("costo_total")),
                        EstadoOperacionFGEM.valueOf(rsFGEM.getString("estado")),
                        rsFGEM.getString("observaciones"),
                        rsFGEM.getString("registrado_por"),
                        FechasFGEM.desdeTextoFGEM(rsFGEM.getString("registrado_en")),
                        rsFGEM.getString("motivo_anulacion"),
                        rsFGEM.getString("anulado_por"),
                        FechasFGEM.desdeTextoFGEM(rsFGEM.getString("anulado_en")),
                        rsFGEM.getInt("items"),
                        List.of(),
                        CondicionPagoFGEM.valueOf(rsFGEM.getString("condicion")),
                        SqlFGEM.enteroOpcionalFGEM(rsFGEM, "cuenta_id"),
                        rsFGEM.getString("cuenta"),
                        FechasFGEM.fechaDesdeTextoFGEM(rsFGEM.getString("vencimiento")),
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("pagado"))));
            }
        }
        return operacionesFGEM;
    }
}
