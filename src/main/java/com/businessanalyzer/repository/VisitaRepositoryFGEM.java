package com.businessanalyzer.repository;

import com.businessanalyzer.exception.DataAccessExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.AspectoInspeccionFGEM;
import com.businessanalyzer.model.CompromisoFGEM;
import com.businessanalyzer.model.ConclusionVisitaFGEM;
import com.businessanalyzer.model.DatosVisitaFGEM;
import com.businessanalyzer.model.EstadoCompromisoFGEM;
import com.businessanalyzer.model.FotoVisitaFGEM;
import com.businessanalyzer.model.ResultadoAspectoFGEM;
import com.businessanalyzer.model.TipoVisitaFGEM;
import com.businessanalyzer.model.VisitaFGEM;
import com.businessanalyzer.util.FechasFGEM;

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

public class VisitaRepositoryFGEM {

    private static final String CONSULTA_FGEM = """
            SELECT id, empresa_id, fecha, tipo, inspector, lugar, latitud, longitud, atendido_por, observaciones,
                   conclusion, proxima_visita, estado, motivo_anulacion, registrado_por
            FROM visita""";

    private static final String CONSULTA_COMPROMISOS_FGEM = """
            SELECT k.id, k.empresa_id, k.visita_id, v.fecha AS fecha_visita, k.descripcion, k.plazo, k.estado,
                   k.fecha_verificacion, k.visita_verificacion_id, k.observacion, k.verificado_por
            FROM compromiso k JOIN visita v ON v.id = k.visita_id""";

    public List<VisitaFGEM> listarFGEM(Connection cFGEM, long empresaIdFGEM, boolean incluirAnuladasFGEM) throws SQLException {
        String sqlFGEM = CONSULTA_FGEM + " WHERE empresa_id = ?" + (incluirAnuladasFGEM ? "" : " AND estado = 'REGISTRADA'")
                + " ORDER BY fecha DESC, id DESC";
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(sqlFGEM)) {
            psFGEM.setLong(1, empresaIdFGEM);
            return mapearFGEM(cFGEM, psFGEM);
        }
    }

    public Optional<VisitaFGEM> buscarFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(CONSULTA_FGEM + " WHERE id = ?")) {
            psFGEM.setLong(1, idFGEM);
            List<VisitaFGEM> rFGEM = mapearFGEM(cFGEM, psFGEM);
            return rFGEM.isEmpty() ? Optional.empty() : Optional.of(rFGEM.get(0));
        }
    }

    public long insertarFGEM(Connection cFGEM, long empresaIdFGEM, DatosVisitaFGEM dFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM)
            throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                INSERT INTO visita (fecha, tipo, inspector, lugar, latitud, longitud, atendido_por, observaciones,
                                    conclusion, proxima_visita, empresa_id, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            asignarFGEM(psFGEM, dFGEM);
            psFGEM.setLong(11, empresaIdFGEM);
            psFGEM.setString(12, usuarioFGEM);
            psFGEM.setString(13, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.executeUpdate();
            long idFGEM = claveFGEM(psFGEM);
            guardarAspectosFGEM(cFGEM, idFGEM, dFGEM);
            return idFGEM;
        }
    }

    public void actualizarFGEM(Connection cFGEM, long idFGEM, DatosVisitaFGEM dFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                UPDATE visita SET fecha = ?, tipo = ?, inspector = ?, lugar = ?, latitud = ?, longitud = ?,
                                  atendido_por = ?, observaciones = ?, conclusion = ?, proxima_visita = ?,
                                  actualizado_por = ?, actualizado_en = ?
                WHERE id = ? AND estado = 'REGISTRADA'""")) {
            asignarFGEM(psFGEM, dFGEM);
            psFGEM.setString(11, usuarioFGEM);
            psFGEM.setString(12, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(13, idFGEM);
            if (psFGEM.executeUpdate() != 1) {
                throw new ValidacionExceptionFGEM("La visita ya no existe o está anulada.");
            }
        }
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("DELETE FROM visita_aspecto WHERE visita_id = ?")) {
            psFGEM.setLong(1, idFGEM);
            psFGEM.executeUpdate();
        }
        guardarAspectosFGEM(cFGEM, idFGEM, dFGEM);
    }

    public void anularFGEM(Connection cFGEM, long idFGEM, String motivoFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "UPDATE visita SET estado = 'ANULADA', motivo_anulacion = ? WHERE id = ? AND estado = 'REGISTRADA'")) {
            psFGEM.setString(1, motivoFGEM);
            psFGEM.setLong(2, idFGEM);
            if (psFGEM.executeUpdate() != 1) {
                throw new ValidacionExceptionFGEM("La visita ya no existe o está anulada.");
            }
        }
    }

    public List<CompromisoFGEM> listarCompromisosFGEM(Connection cFGEM, long empresaIdFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(CONSULTA_COMPROMISOS_FGEM
                + " WHERE k.empresa_id = ? AND v.estado = 'REGISTRADA' ORDER BY v.fecha DESC, k.id")) {
            psFGEM.setLong(1, empresaIdFGEM);
            return mapearCompromisosFGEM(psFGEM);
        }
    }

    public Optional<CompromisoFGEM> buscarCompromisoFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(CONSULTA_COMPROMISOS_FGEM + " WHERE k.id = ?")) {
            psFGEM.setLong(1, idFGEM);
            List<CompromisoFGEM> rFGEM = mapearCompromisosFGEM(psFGEM);
            return rFGEM.isEmpty() ? Optional.empty() : Optional.of(rFGEM.get(0));
        }
    }

    public void insertarCompromisoFGEM(Connection cFGEM, long empresaIdFGEM, long visitaIdFGEM, String descripcionFGEM, LocalDate plazoFGEM)
            throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "INSERT INTO compromiso (empresa_id, visita_id, descripcion, plazo) VALUES (?, ?, ?, ?)")) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setLong(2, visitaIdFGEM);
            psFGEM.setString(3, descripcionFGEM);
            psFGEM.setString(4, FechasFGEM.fechaATextoFGEM(plazoFGEM));
            psFGEM.executeUpdate();
        }
    }

    public void verificarCompromisoFGEM(Connection cFGEM, long idFGEM, EstadoCompromisoFGEM estadoFGEM, LocalDate fechaFGEM,
                                    Long visitaVerificacionIdFGEM, String observacionFGEM, String usuarioFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                UPDATE compromiso SET estado = ?, fecha_verificacion = ?, visita_verificacion_id = ?, observacion = ?,
                                      verificado_por = ?
                WHERE id = ?""")) {
            psFGEM.setString(1, estadoFGEM.name());
            psFGEM.setString(2, FechasFGEM.fechaATextoFGEM(fechaFGEM));
            if (visitaVerificacionIdFGEM == null) {
                psFGEM.setNull(3, Types.INTEGER);
            } else {
                psFGEM.setLong(3, visitaVerificacionIdFGEM);
            }
            psFGEM.setString(4, observacionFGEM);
            psFGEM.setString(5, estadoFGEM == EstadoCompromisoFGEM.PENDIENTE ? null : usuarioFGEM);
            psFGEM.setLong(6, idFGEM);
            psFGEM.executeUpdate();
        }
    }

    public List<FotoVisitaFGEM> listarFotosFGEM(Connection cFGEM, long visitaIdFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "SELECT id, visita_id, archivo, descripcion FROM visita_foto WHERE visita_id = ? ORDER BY id")) {
            psFGEM.setLong(1, visitaIdFGEM);
            List<FotoVisitaFGEM> fotosFGEM = new ArrayList<>();
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                while (rsFGEM.next()) {
                    fotosFGEM.add(new FotoVisitaFGEM(rsFGEM.getLong(1), rsFGEM.getLong(2), rsFGEM.getString(3), rsFGEM.getString(4)));
                }
            }
            return fotosFGEM;
        }
    }

    public Optional<FotoVisitaFGEM> buscarFotoFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "SELECT id, visita_id, archivo, descripcion FROM visita_foto WHERE id = ?")) {
            psFGEM.setLong(1, idFGEM);
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                return rsFGEM.next() ? Optional.of(new FotoVisitaFGEM(rsFGEM.getLong(1), rsFGEM.getLong(2), rsFGEM.getString(3),
                        rsFGEM.getString(4))) : Optional.empty();
            }
        }
    }

    public long insertarFotoFGEM(Connection cFGEM, long visitaIdFGEM, String archivoFGEM, String descripcionFGEM, LocalDateTime ahoraFGEM)
            throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "INSERT INTO visita_foto (visita_id, archivo, descripcion, registrado_en) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            psFGEM.setLong(1, visitaIdFGEM);
            psFGEM.setString(2, archivoFGEM);
            psFGEM.setString(3, descripcionFGEM);
            psFGEM.setString(4, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.executeUpdate();
            return claveFGEM(psFGEM);
        }
    }

    public void eliminarFotoFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("DELETE FROM visita_foto WHERE id = ?")) {
            psFGEM.setLong(1, idFGEM);
            psFGEM.executeUpdate();
        }
    }

    private static void asignarFGEM(PreparedStatement psFGEM, DatosVisitaFGEM dFGEM) throws SQLException {
        psFGEM.setString(1, FechasFGEM.fechaATextoFGEM(dFGEM.fechaFGEM()));
        psFGEM.setString(2, dFGEM.tipoFGEM().name());
        psFGEM.setString(3, dFGEM.inspectorFGEM());
        psFGEM.setString(4, dFGEM.lugarFGEM());
        decimalFGEM(psFGEM, 5, dFGEM.latitudFGEM());
        decimalFGEM(psFGEM, 6, dFGEM.longitudFGEM());
        psFGEM.setString(7, dFGEM.atendidoPorFGEM());
        psFGEM.setString(8, dFGEM.observacionesFGEM());
        psFGEM.setString(9, dFGEM.conclusionFGEM().name());
        psFGEM.setString(10, FechasFGEM.fechaATextoFGEM(dFGEM.proximaVisitaFGEM()));
    }

    private static void decimalFGEM(PreparedStatement psFGEM, int indiceFGEM, Double valorFGEM) throws SQLException {
        if (valorFGEM == null) {
            psFGEM.setNull(indiceFGEM, Types.REAL);
        } else {
            psFGEM.setDouble(indiceFGEM, valorFGEM);
        }
    }

    private static void guardarAspectosFGEM(Connection cFGEM, long visitaIdFGEM, DatosVisitaFGEM dFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "INSERT INTO visita_aspecto (visita_id, aspecto, resultado, observacion) VALUES (?, ?, ?, ?)")) {
            for (Map.Entry<AspectoInspeccionFGEM, ResultadoAspectoFGEM> eFGEM : dFGEM.aspectosFGEM().entrySet()) {
                psFGEM.setLong(1, visitaIdFGEM);
                psFGEM.setString(2, eFGEM.getKey().name());
                psFGEM.setString(3, eFGEM.getValue().name());
                psFGEM.setString(4, dFGEM.notasFGEM().get(eFGEM.getKey()));
                psFGEM.addBatch();
            }
            psFGEM.executeBatch();
        }
    }

    private static long claveFGEM(PreparedStatement psFGEM) throws SQLException {
        try (ResultSet clavesFGEM = psFGEM.getGeneratedKeys()) {
            if (!clavesFGEM.next()) {
                throw new DataAccessExceptionFGEM("No se obtuvo el identificador del registro creado");
            }
            return clavesFGEM.getLong(1);
        }
    }

    private static Double leerDecimalFGEM(ResultSet rsFGEM, String columnaFGEM) throws SQLException {
        double valorFGEM = rsFGEM.getDouble(columnaFGEM);
        return rsFGEM.wasNull() ? null : valorFGEM;
    }

    private static List<VisitaFGEM> mapearFGEM(Connection cFGEM, PreparedStatement psFGEM) throws SQLException {
        List<VisitaFGEM> visitasFGEM = new ArrayList<>();
        try (ResultSet rsFGEM = psFGEM.executeQuery()) {
            while (rsFGEM.next()) {
                long idFGEM = rsFGEM.getLong("id");
                visitasFGEM.add(new VisitaFGEM(idFGEM, rsFGEM.getLong("empresa_id"), FechasFGEM.fechaDesdeTextoFGEM(rsFGEM.getString("fecha")),
                        TipoVisitaFGEM.valueOf(rsFGEM.getString("tipo")), rsFGEM.getString("inspector"), rsFGEM.getString("lugar"),
                        leerDecimalFGEM(rsFGEM, "latitud"), leerDecimalFGEM(rsFGEM, "longitud"), rsFGEM.getString("atendido_por"),
                        rsFGEM.getString("observaciones"), ConclusionVisitaFGEM.valueOf(rsFGEM.getString("conclusion")),
                        FechasFGEM.fechaDesdeTextoFGEM(rsFGEM.getString("proxima_visita")), "ANULADA".equals(rsFGEM.getString("estado")),
                        rsFGEM.getString("motivo_anulacion"), rsFGEM.getString("registrado_por"), Map.of(), Map.of()));
            }
        }
        List<VisitaFGEM> completasFGEM = new ArrayList<>();
        for (VisitaFGEM vFGEM : visitasFGEM) {
            Map<AspectoInspeccionFGEM, ResultadoAspectoFGEM> aspectosFGEM = new EnumMap<>(AspectoInspeccionFGEM.class);
            Map<AspectoInspeccionFGEM, String> notasFGEM = new EnumMap<>(AspectoInspeccionFGEM.class);
            try (PreparedStatement paFGEM = cFGEM.prepareStatement(
                    "SELECT aspecto, resultado, observacion FROM visita_aspecto WHERE visita_id = ?")) {
                paFGEM.setLong(1, vFGEM.idFGEM());
                try (ResultSet raFGEM = paFGEM.executeQuery()) {
                    while (raFGEM.next()) {
                        AspectoInspeccionFGEM aFGEM = AspectoInspeccionFGEM.valueOf(raFGEM.getString(1));
                        aspectosFGEM.put(aFGEM, ResultadoAspectoFGEM.valueOf(raFGEM.getString(2)));
                        if (raFGEM.getString(3) != null) {
                            notasFGEM.put(aFGEM, raFGEM.getString(3));
                        }
                    }
                }
            }
            completasFGEM.add(new VisitaFGEM(vFGEM.idFGEM(), vFGEM.empresaIdFGEM(), vFGEM.fechaFGEM(), vFGEM.tipoFGEM(), vFGEM.inspectorFGEM(), vFGEM.lugarFGEM(), vFGEM.latitudFGEM(),
                    vFGEM.longitudFGEM(), vFGEM.atendidoPorFGEM(), vFGEM.observacionesFGEM(), vFGEM.conclusionFGEM(), vFGEM.proximaVisitaFGEM(), vFGEM.anuladaFGEM(),
                    vFGEM.motivoAnulacionFGEM(), vFGEM.registradoPorFGEM(), aspectosFGEM, notasFGEM));
        }
        return completasFGEM;
    }

    private static List<CompromisoFGEM> mapearCompromisosFGEM(PreparedStatement psFGEM) throws SQLException {
        List<CompromisoFGEM> listaFGEM = new ArrayList<>();
        try (ResultSet rsFGEM = psFGEM.executeQuery()) {
            while (rsFGEM.next()) {
                long verifFGEM = rsFGEM.getLong("visita_verificacion_id");
                Long verificacionFGEM = rsFGEM.wasNull() ? null : verifFGEM;
                listaFGEM.add(new CompromisoFGEM(rsFGEM.getLong("id"), rsFGEM.getLong("empresa_id"), rsFGEM.getLong("visita_id"),
                        FechasFGEM.fechaDesdeTextoFGEM(rsFGEM.getString("fecha_visita")), rsFGEM.getString("descripcion"),
                        FechasFGEM.fechaDesdeTextoFGEM(rsFGEM.getString("plazo")), EstadoCompromisoFGEM.valueOf(rsFGEM.getString("estado")),
                        FechasFGEM.fechaDesdeTextoFGEM(rsFGEM.getString("fecha_verificacion")), verificacionFGEM,
                        rsFGEM.getString("observacion"), rsFGEM.getString("verificado_por")));
            }
        }
        return listaFGEM;
    }
}
