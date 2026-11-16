package com.businessanalyzer.repository;

import com.businessanalyzer.exception.DataAccessExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CampaniaFGEM;
import com.businessanalyzer.model.CosechaFGEM;
import com.businessanalyzer.model.CostoCampaniaFGEM;
import com.businessanalyzer.model.CultivoFGEM;
import com.businessanalyzer.model.DatosCampaniaFGEM;
import com.businessanalyzer.model.DatosParcelaFGEM;
import com.businessanalyzer.model.DestinoCosechaFGEM;
import com.businessanalyzer.model.EstadoCampaniaFGEM;
import com.businessanalyzer.model.ParcelaFGEM;
import com.businessanalyzer.model.RubroCostoFGEM;
import com.businessanalyzer.model.TenenciaFGEM;
import com.businessanalyzer.util.FechasFGEM;
import com.businessanalyzer.util.MontosFGEM;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Acceso a las tablas {@code parcela}, {@code campania}, {@code costo_campania} y {@code cosecha}. */
public class AgriculturaRepositoryFGEM {

    private static final String CONSULTA_PARCELAS_FGEM = """
            SELECT id, empresa_id, nombre, area, tenencia, riego, ubicacion, activa FROM parcela""";

    private static final String CONSULTA_CAMPANIAS_FGEM = """
            SELECT c.id, c.empresa_id, c.parcela_id, p.nombre AS parcela_nombre, c.cultivo, c.variedad, c.fecha_siembra,
                   c.area, c.rendimiento_referencia, c.precio_chacra, c.estado, c.fecha_cierre, c.motivo, c.observacion,
                   c.registrado_por
            FROM campania c JOIN parcela p ON p.id = c.parcela_id""";

    private static final String CONSULTA_COSTOS_FGEM = """
            SELECT k.id, k.campania_id, k.fecha, k.rubro, k.descripcion, k.monto, k.estado, k.registrado_por
            FROM costo_campania k JOIN campania c ON c.id = k.campania_id""";

    private static final String CONSULTA_COSECHAS_FGEM = """
            SELECT h.id, h.campania_id, h.fecha, h.cantidad, h.destino, h.precio, h.observacion, h.estado, h.registrado_por
            FROM cosecha h JOIN campania c ON c.id = h.campania_id""";

    // ------------------------------------------------------------------ parcelas

    public List<ParcelaFGEM> listarParcelasFGEM(Connection cFGEM, long empresaIdFGEM, boolean incluirInactivasFGEM) throws SQLException {
        String sqlFGEM = CONSULTA_PARCELAS_FGEM + " WHERE empresa_id = ?" + (incluirInactivasFGEM ? "" : " AND activa = 1")
                + " ORDER BY nombre COLLATE NOCASE";
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(sqlFGEM)) {
            psFGEM.setLong(1, empresaIdFGEM);
            return mapearParcelasFGEM(psFGEM);
        }
    }

    public Optional<ParcelaFGEM> buscarParcelaFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(CONSULTA_PARCELAS_FGEM + " WHERE id = ?")) {
            psFGEM.setLong(1, idFGEM);
            List<ParcelaFGEM> rFGEM = mapearParcelasFGEM(psFGEM);
            return rFGEM.isEmpty() ? Optional.empty() : Optional.of(rFGEM.get(0));
        }
    }

    public boolean existeParcelaFGEM(Connection cFGEM, long empresaIdFGEM, String nombreFGEM, long excluirIdFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "SELECT 1 FROM parcela WHERE empresa_id = ? AND nombre = ? COLLATE NOCASE AND id <> ?")) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setString(2, nombreFGEM);
            psFGEM.setLong(3, excluirIdFGEM);
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                return rsFGEM.next();
            }
        }
    }

    public long insertarParcelaFGEM(Connection cFGEM, long empresaIdFGEM, DatosParcelaFGEM dFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM)
            throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                INSERT INTO parcela (nombre, area, tenencia, riego, ubicacion, empresa_id, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            asignarParcelaFGEM(psFGEM, dFGEM);
            psFGEM.setLong(6, empresaIdFGEM);
            psFGEM.setString(7, usuarioFGEM);
            psFGEM.setString(8, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.executeUpdate();
            return claveFGEM(psFGEM);
        }
    }

    public void actualizarParcelaFGEM(Connection cFGEM, long idFGEM, DatosParcelaFGEM dFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM)
            throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                UPDATE parcela SET nombre = ?, area = ?, tenencia = ?, riego = ?, ubicacion = ?,
                                   actualizado_por = ?, actualizado_en = ?
                WHERE id = ?""")) {
            asignarParcelaFGEM(psFGEM, dFGEM);
            psFGEM.setString(6, usuarioFGEM);
            psFGEM.setString(7, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(8, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate(), "La parcela ya no existe.");
        }
    }

    public void cambiarParcelaActivaFGEM(Connection cFGEM, long idFGEM, boolean activaFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("UPDATE parcela SET activa = ? WHERE id = ?")) {
            psFGEM.setInt(1, activaFGEM ? 1 : 0);
            psFGEM.setLong(2, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate(), "La parcela ya no existe.");
        }
    }

    // ------------------------------------------------------------------ campañas

    public List<CampaniaFGEM> listarCampaniasFGEM(Connection cFGEM, long empresaIdFGEM, boolean incluirAnuladasFGEM) throws SQLException {
        String sqlFGEM = CONSULTA_CAMPANIAS_FGEM + " WHERE c.empresa_id = ?"
                + (incluirAnuladasFGEM ? "" : " AND c.estado <> 'ANULADA'")
                + " ORDER BY c.fecha_siembra DESC, c.id DESC";
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(sqlFGEM)) {
            psFGEM.setLong(1, empresaIdFGEM);
            return mapearCampaniasFGEM(psFGEM);
        }
    }

    public Optional<CampaniaFGEM> buscarCampaniaFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(CONSULTA_CAMPANIAS_FGEM + " WHERE c.id = ?")) {
            psFGEM.setLong(1, idFGEM);
            List<CampaniaFGEM> rFGEM = mapearCampaniasFGEM(psFGEM);
            return rFGEM.isEmpty() ? Optional.empty() : Optional.of(rFGEM.get(0));
        }
    }

    public long insertarCampaniaFGEM(Connection cFGEM, long empresaIdFGEM, DatosCampaniaFGEM dFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM)
            throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                INSERT INTO campania (parcela_id, cultivo, variedad, fecha_siembra, area, rendimiento_referencia,
                                      precio_chacra, observacion, empresa_id, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            asignarCampaniaFGEM(psFGEM, dFGEM);
            psFGEM.setLong(9, empresaIdFGEM);
            psFGEM.setString(10, usuarioFGEM);
            psFGEM.setString(11, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.executeUpdate();
            return claveFGEM(psFGEM);
        }
    }

    public void actualizarCampaniaFGEM(Connection cFGEM, long idFGEM, DatosCampaniaFGEM dFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM)
            throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                UPDATE campania SET parcela_id = ?, cultivo = ?, variedad = ?, fecha_siembra = ?, area = ?,
                                    rendimiento_referencia = ?, precio_chacra = ?, observacion = ?,
                                    actualizado_por = ?, actualizado_en = ?
                WHERE id = ? AND estado <> 'ANULADA'""")) {
            asignarCampaniaFGEM(psFGEM, dFGEM);
            psFGEM.setString(9, usuarioFGEM);
            psFGEM.setString(10, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(11, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate(), "La campaña ya no existe o está anulada.");
        }
    }

    public void cambiarEstadoFGEM(Connection cFGEM, long idFGEM, EstadoCampaniaFGEM estadoFGEM, LocalDate fechaCierreFGEM, String motivoFGEM,
                              String usuarioFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                UPDATE campania SET estado = ?, fecha_cierre = ?, motivo = ?, actualizado_por = ?, actualizado_en = ?
                WHERE id = ?""")) {
            psFGEM.setString(1, estadoFGEM.name());
            psFGEM.setString(2, FechasFGEM.fechaATextoFGEM(fechaCierreFGEM));
            psFGEM.setString(3, motivoFGEM);
            psFGEM.setString(4, usuarioFGEM);
            psFGEM.setString(5, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(6, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate(), "La campaña ya no existe.");
        }
    }

    // ------------------------------------------------------------------ costos y cosechas

    /** Costos vigentes o todos de las campañas de la empresa (o de una sola campaña si {@code campaniaId} no es nulo). */
    public List<CostoCampaniaFGEM> listarCostosFGEM(Connection cFGEM, long empresaIdFGEM, Long campaniaIdFGEM, boolean incluirAnuladosFGEM)
            throws SQLException {
        String sqlFGEM = CONSULTA_COSTOS_FGEM + " WHERE c.empresa_id = ?" + (campaniaIdFGEM == null ? "" : " AND k.campania_id = ?")
                + (incluirAnuladosFGEM ? "" : " AND k.estado = 'REGISTRADO'") + " ORDER BY k.fecha DESC, k.id DESC";
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(sqlFGEM)) {
            psFGEM.setLong(1, empresaIdFGEM);
            if (campaniaIdFGEM != null) {
                psFGEM.setLong(2, campaniaIdFGEM);
            }
            List<CostoCampaniaFGEM> listaFGEM = new ArrayList<>();
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                while (rsFGEM.next()) {
                    listaFGEM.add(new CostoCampaniaFGEM(rsFGEM.getLong("id"), rsFGEM.getLong("campania_id"),
                            FechasFGEM.fechaDesdeTextoFGEM(rsFGEM.getString("fecha")), RubroCostoFGEM.valueOf(rsFGEM.getString("rubro")),
                            rsFGEM.getString("descripcion"), MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("monto")),
                            "ANULADO".equals(rsFGEM.getString("estado")), rsFGEM.getString("registrado_por")));
                }
            }
            return listaFGEM;
        }
    }

    public List<CosechaFGEM> listarCosechasFGEM(Connection cFGEM, long empresaIdFGEM, Long campaniaIdFGEM, boolean incluirAnuladasFGEM)
            throws SQLException {
        String sqlFGEM = CONSULTA_COSECHAS_FGEM + " WHERE c.empresa_id = ?" + (campaniaIdFGEM == null ? "" : " AND h.campania_id = ?")
                + (incluirAnuladasFGEM ? "" : " AND h.estado = 'REGISTRADO'") + " ORDER BY h.fecha DESC, h.id DESC";
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(sqlFGEM)) {
            psFGEM.setLong(1, empresaIdFGEM);
            if (campaniaIdFGEM != null) {
                psFGEM.setLong(2, campaniaIdFGEM);
            }
            List<CosechaFGEM> listaFGEM = new ArrayList<>();
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                while (rsFGEM.next()) {
                    listaFGEM.add(new CosechaFGEM(rsFGEM.getLong("id"), rsFGEM.getLong("campania_id"),
                            FechasFGEM.fechaDesdeTextoFGEM(rsFGEM.getString("fecha")), MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("cantidad")),
                            DestinoCosechaFGEM.valueOf(rsFGEM.getString("destino")), leerMontoFGEM(rsFGEM, "precio"),
                            rsFGEM.getString("observacion"), "ANULADO".equals(rsFGEM.getString("estado")),
                            rsFGEM.getString("registrado_por")));
                }
            }
            return listaFGEM;
        }
    }

    public long insertarCostoFGEM(Connection cFGEM, long campaniaIdFGEM, LocalDate fechaFGEM, RubroCostoFGEM rubroFGEM, String descripcionFGEM,
                              BigDecimal montoFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                INSERT INTO costo_campania (campania_id, fecha, rubro, descripcion, monto, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            psFGEM.setLong(1, campaniaIdFGEM);
            psFGEM.setString(2, FechasFGEM.fechaATextoFGEM(fechaFGEM));
            psFGEM.setString(3, rubroFGEM.name());
            psFGEM.setString(4, descripcionFGEM);
            psFGEM.setLong(5, MontosFGEM.aEnteroFGEM(montoFGEM));
            psFGEM.setString(6, usuarioFGEM);
            psFGEM.setString(7, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.executeUpdate();
            return claveFGEM(psFGEM);
        }
    }

    public long insertarCosechaFGEM(Connection cFGEM, long campaniaIdFGEM, LocalDate fechaFGEM, BigDecimal cantidadFGEM,
                                DestinoCosechaFGEM destinoFGEM, BigDecimal precioFGEM, String observacionFGEM, String usuarioFGEM,
                                LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                INSERT INTO cosecha (campania_id, fecha, cantidad, destino, precio, observacion, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            psFGEM.setLong(1, campaniaIdFGEM);
            psFGEM.setString(2, FechasFGEM.fechaATextoFGEM(fechaFGEM));
            psFGEM.setLong(3, MontosFGEM.aEnteroFGEM(cantidadFGEM));
            psFGEM.setString(4, destinoFGEM.name());
            montoOpcionalFGEM(psFGEM, 5, precioFGEM);
            psFGEM.setString(6, observacionFGEM);
            psFGEM.setString(7, usuarioFGEM);
            psFGEM.setString(8, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.executeUpdate();
            return claveFGEM(psFGEM);
        }
    }

    /** Anula un costo o una cosecha; devuelve la campaña a la que pertenecía o vacío si no existe o ya estaba anulado. */
    public Optional<Long> anularRegistroFGEM(Connection cFGEM, String tablaFGEM, long idFGEM, long empresaIdFGEM, String usuarioFGEM,
                                         LocalDateTime ahoraFGEM) throws SQLException {
        if (!tablaFGEM.equals("costo_campania") && !tablaFGEM.equals("cosecha")) {
            throw new IllegalArgumentException(tablaFGEM);
        }
        long campaniaIdFGEM;
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("SELECT t.campania_id FROM " + tablaFGEM + " t "
                + "JOIN campania c ON c.id = t.campania_id WHERE t.id = ? AND c.empresa_id = ? AND t.estado = 'REGISTRADO'")) {
            psFGEM.setLong(1, idFGEM);
            psFGEM.setLong(2, empresaIdFGEM);
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                if (!rsFGEM.next()) {
                    return Optional.empty();
                }
                campaniaIdFGEM = rsFGEM.getLong(1);
            }
        }
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("UPDATE " + tablaFGEM
                + " SET estado = 'ANULADO', anulado_por = ?, anulado_en = ? WHERE id = ?")) {
            psFGEM.setString(1, usuarioFGEM);
            psFGEM.setString(2, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(3, idFGEM);
            psFGEM.executeUpdate();
        }
        return Optional.of(campaniaIdFGEM);
    }

    // ------------------------------------------------------------------ utilidades

    private static void asignarParcelaFGEM(PreparedStatement psFGEM, DatosParcelaFGEM dFGEM) throws SQLException {
        psFGEM.setString(1, dFGEM.nombreFGEM());
        psFGEM.setLong(2, MontosFGEM.aEnteroFGEM(dFGEM.areaFGEM()));
        psFGEM.setString(3, dFGEM.tenenciaFGEM().name());
        psFGEM.setInt(4, dFGEM.riegoFGEM() ? 1 : 0);
        psFGEM.setString(5, dFGEM.ubicacionFGEM());
    }

    private static void asignarCampaniaFGEM(PreparedStatement psFGEM, DatosCampaniaFGEM dFGEM) throws SQLException {
        psFGEM.setLong(1, dFGEM.parcelaIdFGEM());
        psFGEM.setString(2, dFGEM.cultivoFGEM().name());
        psFGEM.setString(3, dFGEM.variedadFGEM());
        psFGEM.setString(4, FechasFGEM.fechaATextoFGEM(dFGEM.fechaSiembraFGEM()));
        psFGEM.setLong(5, MontosFGEM.aEnteroFGEM(dFGEM.areaFGEM()));
        montoOpcionalFGEM(psFGEM, 6, dFGEM.rendimientoReferenciaFGEM());
        montoOpcionalFGEM(psFGEM, 7, dFGEM.precioChacraFGEM());
        psFGEM.setString(8, dFGEM.observacionFGEM());
    }

    private static void montoOpcionalFGEM(PreparedStatement psFGEM, int indiceFGEM, BigDecimal valorFGEM) throws SQLException {
        if (valorFGEM == null) {
            psFGEM.setNull(indiceFGEM, Types.INTEGER);
        } else {
            psFGEM.setLong(indiceFGEM, MontosFGEM.aEnteroFGEM(valorFGEM));
        }
    }

    private static BigDecimal leerMontoFGEM(ResultSet rsFGEM, String columnaFGEM) throws SQLException {
        long valorFGEM = rsFGEM.getLong(columnaFGEM);
        return rsFGEM.wasNull() ? null : MontosFGEM.desdeEnteroFGEM(valorFGEM);
    }

    private static long claveFGEM(PreparedStatement psFGEM) throws SQLException {
        try (ResultSet clavesFGEM = psFGEM.getGeneratedKeys()) {
            if (!clavesFGEM.next()) {
                throw new DataAccessExceptionFGEM("No se obtuvo el identificador del registro creado");
            }
            return clavesFGEM.getLong(1);
        }
    }

    private static List<ParcelaFGEM> mapearParcelasFGEM(PreparedStatement psFGEM) throws SQLException {
        List<ParcelaFGEM> listaFGEM = new ArrayList<>();
        try (ResultSet rsFGEM = psFGEM.executeQuery()) {
            while (rsFGEM.next()) {
                listaFGEM.add(new ParcelaFGEM(rsFGEM.getLong("id"), rsFGEM.getLong("empresa_id"), rsFGEM.getString("nombre"),
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("area")), TenenciaFGEM.valueOf(rsFGEM.getString("tenencia")),
                        rsFGEM.getInt("riego") == 1, rsFGEM.getString("ubicacion"), rsFGEM.getInt("activa") == 1));
            }
        }
        return listaFGEM;
    }

    private static List<CampaniaFGEM> mapearCampaniasFGEM(PreparedStatement psFGEM) throws SQLException {
        List<CampaniaFGEM> listaFGEM = new ArrayList<>();
        try (ResultSet rsFGEM = psFGEM.executeQuery()) {
            while (rsFGEM.next()) {
                listaFGEM.add(new CampaniaFGEM(rsFGEM.getLong("id"), rsFGEM.getLong("empresa_id"), rsFGEM.getLong("parcela_id"),
                        rsFGEM.getString("parcela_nombre"), CultivoFGEM.valueOf(rsFGEM.getString("cultivo")),
                        rsFGEM.getString("variedad"), FechasFGEM.fechaDesdeTextoFGEM(rsFGEM.getString("fecha_siembra")),
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("area")), leerMontoFGEM(rsFGEM, "rendimiento_referencia"),
                        leerMontoFGEM(rsFGEM, "precio_chacra"), EstadoCampaniaFGEM.valueOf(rsFGEM.getString("estado")),
                        FechasFGEM.fechaDesdeTextoFGEM(rsFGEM.getString("fecha_cierre")), rsFGEM.getString("motivo"),
                        rsFGEM.getString("observacion"), rsFGEM.getString("registrado_por")));
            }
        }
        return listaFGEM;
    }

    private static void exigirUnaFilaFGEM(int filasFGEM, String mensajeFGEM) {
        if (filasFGEM != 1) {
            throw new ValidacionExceptionFGEM(mensajeFGEM);
        }
    }
}
