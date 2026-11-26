package com.businessanalyzer.repository;

import com.businessanalyzer.exception.DataAccessExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CategoriaGanadoFGEM;
import com.businessanalyzer.model.DatosMovimientoGanadoFGEM;
import com.businessanalyzer.model.DatosProduccionFGEM;
import com.businessanalyzer.model.EspecieFGEM;
import com.businessanalyzer.model.MovimientoGanadoFGEM;
import com.businessanalyzer.model.ProduccionGanaderaFGEM;
import com.businessanalyzer.model.ProductoGanaderoFGEM;
import com.businessanalyzer.model.TipoMovimientoGanadoFGEM;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GanaderiaRepositoryFGEM {

    private static final String CONSULTA_MOVIMIENTOS_FGEM = """
            SELECT id, empresa_id, fecha, tipo, categoria, cantidad, categoria_destino, monto, causa, observacion,
                   estado, motivo_anulacion, registrado_por, registrado_en
            FROM movimiento_ganado""";

    private static final String CONSULTA_PRODUCCION_FGEM = """
            SELECT id, empresa_id, especie, producto, fecha, dias, cantidad, animales, precio_unitario, observacion,
                   estado, registrado_por, registrado_en
            FROM produccion_ganadera""";

    public List<MovimientoGanadoFGEM> listarMovimientosFGEM(Connection cFGEM, long empresaIdFGEM, LocalDate hastaFGEM,
                                                    boolean incluirAnuladosFGEM) throws SQLException {
        String sqlFGEM = CONSULTA_MOVIMIENTOS_FGEM + " WHERE empresa_id = ? AND fecha <= ?"
                + (incluirAnuladosFGEM ? "" : " AND estado = 'REGISTRADO'")
                + " ORDER BY fecha, id";
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(sqlFGEM)) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setString(2, FechasFGEM.fechaATextoFGEM(hastaFGEM));
            return mapearMovimientosFGEM(psFGEM);
        }
    }

    public Optional<MovimientoGanadoFGEM> buscarMovimientoFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(CONSULTA_MOVIMIENTOS_FGEM + " WHERE id = ?")) {
            psFGEM.setLong(1, idFGEM);
            List<MovimientoGanadoFGEM> rFGEM = mapearMovimientosFGEM(psFGEM);
            return rFGEM.isEmpty() ? Optional.empty() : Optional.of(rFGEM.get(0));
        }
    }

    public long insertarMovimientoFGEM(Connection cFGEM, long empresaIdFGEM, DatosMovimientoGanadoFGEM dFGEM, String usuarioFGEM,
                                   LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                INSERT INTO movimiento_ganado (empresa_id, fecha, tipo, categoria, cantidad, categoria_destino, monto,
                                               causa, observacion, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setString(2, FechasFGEM.fechaATextoFGEM(dFGEM.fechaFGEM()));
            psFGEM.setString(3, dFGEM.tipoFGEM().name());
            psFGEM.setString(4, dFGEM.categoriaFGEM().name());
            psFGEM.setInt(5, dFGEM.cantidadFGEM());
            psFGEM.setString(6, dFGEM.categoriaDestinoFGEM() == null ? null : dFGEM.categoriaDestinoFGEM().name());
            montoOpcionalFGEM(psFGEM, 7, dFGEM.montoFGEM());
            psFGEM.setString(8, dFGEM.causaFGEM());
            psFGEM.setString(9, dFGEM.observacionFGEM());
            psFGEM.setString(10, usuarioFGEM);
            psFGEM.setString(11, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.executeUpdate();
            return claveFGEM(psFGEM);
        }
    }

    public void anularMovimientoFGEM(Connection cFGEM, long idFGEM, String motivoFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM)
            throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                UPDATE movimiento_ganado SET estado = 'ANULADO', motivo_anulacion = ?, anulado_por = ?, anulado_en = ?
                WHERE id = ? AND estado = 'REGISTRADO'""")) {
            psFGEM.setString(1, motivoFGEM);
            psFGEM.setString(2, usuarioFGEM);
            psFGEM.setString(3, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(4, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate(), "El movimiento ya no existe o está anulado. Actualiza la lista.");
        }
    }

    public List<ProduccionGanaderaFGEM> listarProduccionFGEM(Connection cFGEM, long empresaIdFGEM, LocalDate desdeFGEM, LocalDate hastaFGEM,
                                                     boolean incluirAnuladosFGEM) throws SQLException {
        String sqlFGEM = CONSULTA_PRODUCCION_FGEM + " WHERE empresa_id = ? AND fecha BETWEEN ? AND ?"
                + (incluirAnuladosFGEM ? "" : " AND estado = 'REGISTRADO'")
                + " ORDER BY fecha DESC, id DESC";
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(sqlFGEM)) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setString(2, FechasFGEM.fechaATextoFGEM(desdeFGEM));
            psFGEM.setString(3, FechasFGEM.fechaATextoFGEM(hastaFGEM));
            return mapearProduccionFGEM(psFGEM);
        }
    }

    public Optional<ProduccionGanaderaFGEM> buscarProduccionFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(CONSULTA_PRODUCCION_FGEM + " WHERE id = ?")) {
            psFGEM.setLong(1, idFGEM);
            List<ProduccionGanaderaFGEM> rFGEM = mapearProduccionFGEM(psFGEM);
            return rFGEM.isEmpty() ? Optional.empty() : Optional.of(rFGEM.get(0));
        }
    }

    public long insertarProduccionFGEM(Connection cFGEM, long empresaIdFGEM, DatosProduccionFGEM dFGEM, String usuarioFGEM,
                                   LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                INSERT INTO produccion_ganadera (empresa_id, especie, producto, fecha, dias, cantidad, animales,
                                                 precio_unitario, observacion, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setString(2, dFGEM.especieFGEM().name());
            psFGEM.setString(3, dFGEM.productoFGEM().name());
            psFGEM.setString(4, FechasFGEM.fechaATextoFGEM(dFGEM.fechaFGEM()));
            psFGEM.setInt(5, dFGEM.diasFGEM());
            psFGEM.setLong(6, MontosFGEM.aEnteroFGEM(dFGEM.cantidadFGEM()));
            if (dFGEM.animalesFGEM() == null) {
                psFGEM.setNull(7, Types.INTEGER);
            } else {
                psFGEM.setInt(7, dFGEM.animalesFGEM());
            }
            montoOpcionalFGEM(psFGEM, 8, dFGEM.precioUnitarioFGEM());
            psFGEM.setString(9, dFGEM.observacionFGEM());
            psFGEM.setString(10, usuarioFGEM);
            psFGEM.setString(11, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.executeUpdate();
            return claveFGEM(psFGEM);
        }
    }

    public void anularProduccionFGEM(Connection cFGEM, long idFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                UPDATE produccion_ganadera SET estado = 'ANULADO', anulado_por = ?, anulado_en = ?
                WHERE id = ? AND estado = 'REGISTRADO'""")) {
            psFGEM.setString(1, usuarioFGEM);
            psFGEM.setString(2, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(3, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate(), "El registro ya no existe o está anulado. Actualiza la lista.");
        }
    }

    public Map<CategoriaGanadoFGEM, BigDecimal> preciosFGEM(Connection cFGEM, long empresaIdFGEM) throws SQLException {
        Map<CategoriaGanadoFGEM, BigDecimal> preciosFGEM = new EnumMap<>(CategoriaGanadoFGEM.class);
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "SELECT categoria, precio FROM precio_ganado WHERE empresa_id = ?")) {
            psFGEM.setLong(1, empresaIdFGEM);
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                while (rsFGEM.next()) {
                    preciosFGEM.put(CategoriaGanadoFGEM.valueOf(rsFGEM.getString(1)), MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong(2)));
                }
            }
        }
        return preciosFGEM;
    }

    public void guardarPrecioFGEM(Connection cFGEM, long empresaIdFGEM, CategoriaGanadoFGEM categoriaFGEM, BigDecimal precioFGEM,
                              String usuarioFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        if (precioFGEM == null) {
            try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                    "DELETE FROM precio_ganado WHERE empresa_id = ? AND categoria = ?")) {
                psFGEM.setLong(1, empresaIdFGEM);
                psFGEM.setString(2, categoriaFGEM.name());
                psFGEM.executeUpdate();
            }
            return;
        }
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                INSERT INTO precio_ganado (empresa_id, categoria, precio, actualizado_por, actualizado_en)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (empresa_id, categoria) DO UPDATE
                SET precio = excluded.precio, actualizado_por = excluded.actualizado_por,
                    actualizado_en = excluded.actualizado_en""")) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setString(2, categoriaFGEM.name());
            psFGEM.setLong(3, MontosFGEM.aEnteroFGEM(precioFGEM));
            psFGEM.setString(4, usuarioFGEM);
            psFGEM.setString(5, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.executeUpdate();
        }
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

    private static List<MovimientoGanadoFGEM> mapearMovimientosFGEM(PreparedStatement psFGEM) throws SQLException {
        List<MovimientoGanadoFGEM> listaFGEM = new ArrayList<>();
        try (ResultSet rsFGEM = psFGEM.executeQuery()) {
            while (rsFGEM.next()) {
                String destinoFGEM = rsFGEM.getString("categoria_destino");
                listaFGEM.add(new MovimientoGanadoFGEM(
                        rsFGEM.getLong("id"),
                        rsFGEM.getLong("empresa_id"),
                        FechasFGEM.fechaDesdeTextoFGEM(rsFGEM.getString("fecha")),
                        TipoMovimientoGanadoFGEM.valueOf(rsFGEM.getString("tipo")),
                        CategoriaGanadoFGEM.valueOf(rsFGEM.getString("categoria")),
                        rsFGEM.getInt("cantidad"),
                        destinoFGEM == null ? null : CategoriaGanadoFGEM.valueOf(destinoFGEM),
                        leerMontoFGEM(rsFGEM, "monto"),
                        rsFGEM.getString("causa"),
                        rsFGEM.getString("observacion"),
                        "ANULADO".equals(rsFGEM.getString("estado")),
                        rsFGEM.getString("motivo_anulacion"),
                        rsFGEM.getString("registrado_por"),
                        FechasFGEM.desdeTextoFGEM(rsFGEM.getString("registrado_en"))));
            }
        }
        return listaFGEM;
    }

    private static List<ProduccionGanaderaFGEM> mapearProduccionFGEM(PreparedStatement psFGEM) throws SQLException {
        List<ProduccionGanaderaFGEM> listaFGEM = new ArrayList<>();
        try (ResultSet rsFGEM = psFGEM.executeQuery()) {
            while (rsFGEM.next()) {
                int animalesFGEM = rsFGEM.getInt("animales");
                Integer animalesOpcionalFGEM = rsFGEM.wasNull() ? null : animalesFGEM;
                listaFGEM.add(new ProduccionGanaderaFGEM(
                        rsFGEM.getLong("id"),
                        rsFGEM.getLong("empresa_id"),
                        EspecieFGEM.valueOf(rsFGEM.getString("especie")),
                        ProductoGanaderoFGEM.valueOf(rsFGEM.getString("producto")),
                        FechasFGEM.fechaDesdeTextoFGEM(rsFGEM.getString("fecha")),
                        rsFGEM.getInt("dias"),
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("cantidad")),
                        animalesOpcionalFGEM,
                        leerMontoFGEM(rsFGEM, "precio_unitario"),
                        rsFGEM.getString("observacion"),
                        "ANULADO".equals(rsFGEM.getString("estado")),
                        rsFGEM.getString("registrado_por"),
                        FechasFGEM.desdeTextoFGEM(rsFGEM.getString("registrado_en"))));
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
