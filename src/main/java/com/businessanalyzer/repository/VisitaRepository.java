package com.businessanalyzer.repository;

import com.businessanalyzer.exception.DataAccessException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.AspectoInspeccion;
import com.businessanalyzer.model.Compromiso;
import com.businessanalyzer.model.ConclusionVisita;
import com.businessanalyzer.model.DatosVisita;
import com.businessanalyzer.model.EstadoCompromiso;
import com.businessanalyzer.model.FotoVisita;
import com.businessanalyzer.model.ResultadoAspecto;
import com.businessanalyzer.model.TipoVisita;
import com.businessanalyzer.model.Visita;
import com.businessanalyzer.util.Fechas;

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

/** Acceso a las tablas {@code visita}, {@code visita_aspecto}, {@code compromiso} y {@code visita_foto}. */
public class VisitaRepository {

    private static final String CONSULTA = """
            SELECT id, empresa_id, fecha, tipo, inspector, lugar, latitud, longitud, atendido_por, observaciones,
                   conclusion, proxima_visita, estado, motivo_anulacion, registrado_por
            FROM visita""";

    private static final String CONSULTA_COMPROMISOS = """
            SELECT k.id, k.empresa_id, k.visita_id, v.fecha AS fecha_visita, k.descripcion, k.plazo, k.estado,
                   k.fecha_verificacion, k.visita_verificacion_id, k.observacion, k.verificado_por
            FROM compromiso k JOIN visita v ON v.id = k.visita_id""";

    // ------------------------------------------------------------------ visitas

    public List<Visita> listar(Connection c, long empresaId, boolean incluirAnuladas) throws SQLException {
        String sql = CONSULTA + " WHERE empresa_id = ?" + (incluirAnuladas ? "" : " AND estado = 'REGISTRADA'")
                + " ORDER BY fecha DESC, id DESC";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            return mapear(c, ps);
        }
    }

    public Optional<Visita> buscar(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(CONSULTA + " WHERE id = ?")) {
            ps.setLong(1, id);
            List<Visita> r = mapear(c, ps);
            return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
        }
    }

    public long insertar(Connection c, long empresaId, DatosVisita d, String usuario, LocalDateTime ahora)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO visita (fecha, tipo, inspector, lugar, latitud, longitud, atendido_por, observaciones,
                                    conclusion, proxima_visita, empresa_id, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            asignar(ps, d);
            ps.setLong(11, empresaId);
            ps.setString(12, usuario);
            ps.setString(13, Fechas.aTexto(ahora));
            ps.executeUpdate();
            long id = clave(ps);
            guardarAspectos(c, id, d);
            return id;
        }
    }

    public void actualizar(Connection c, long id, DatosVisita d, String usuario, LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE visita SET fecha = ?, tipo = ?, inspector = ?, lugar = ?, latitud = ?, longitud = ?,
                                  atendido_por = ?, observaciones = ?, conclusion = ?, proxima_visita = ?,
                                  actualizado_por = ?, actualizado_en = ?
                WHERE id = ? AND estado = 'REGISTRADA'""")) {
            asignar(ps, d);
            ps.setString(11, usuario);
            ps.setString(12, Fechas.aTexto(ahora));
            ps.setLong(13, id);
            if (ps.executeUpdate() != 1) {
                throw new ValidacionException("La visita ya no existe o está anulada.");
            }
        }
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM visita_aspecto WHERE visita_id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
        guardarAspectos(c, id, d);
    }

    public void anular(Connection c, long id, String motivo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE visita SET estado = 'ANULADA', motivo_anulacion = ? WHERE id = ? AND estado = 'REGISTRADA'")) {
            ps.setString(1, motivo);
            ps.setLong(2, id);
            if (ps.executeUpdate() != 1) {
                throw new ValidacionException("La visita ya no existe o está anulada.");
            }
        }
    }

    // ------------------------------------------------------------------ compromisos

    public List<Compromiso> listarCompromisos(Connection c, long empresaId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(CONSULTA_COMPROMISOS
                + " WHERE k.empresa_id = ? AND v.estado = 'REGISTRADA' ORDER BY v.fecha DESC, k.id")) {
            ps.setLong(1, empresaId);
            return mapearCompromisos(ps);
        }
    }

    public Optional<Compromiso> buscarCompromiso(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(CONSULTA_COMPROMISOS + " WHERE k.id = ?")) {
            ps.setLong(1, id);
            List<Compromiso> r = mapearCompromisos(ps);
            return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
        }
    }

    public void insertarCompromiso(Connection c, long empresaId, long visitaId, String descripcion, LocalDate plazo)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO compromiso (empresa_id, visita_id, descripcion, plazo) VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, empresaId);
            ps.setLong(2, visitaId);
            ps.setString(3, descripcion);
            ps.setString(4, Fechas.fechaATexto(plazo));
            ps.executeUpdate();
        }
    }

    public void verificarCompromiso(Connection c, long id, EstadoCompromiso estado, LocalDate fecha,
                                    Long visitaVerificacionId, String observacion, String usuario) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE compromiso SET estado = ?, fecha_verificacion = ?, visita_verificacion_id = ?, observacion = ?,
                                      verificado_por = ?
                WHERE id = ?""")) {
            ps.setString(1, estado.name());
            ps.setString(2, Fechas.fechaATexto(fecha));
            if (visitaVerificacionId == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setLong(3, visitaVerificacionId);
            }
            ps.setString(4, observacion);
            ps.setString(5, estado == EstadoCompromiso.PENDIENTE ? null : usuario);
            ps.setLong(6, id);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------ fotos

    public List<FotoVisita> listarFotos(Connection c, long visitaId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, visita_id, archivo, descripcion FROM visita_foto WHERE visita_id = ? ORDER BY id")) {
            ps.setLong(1, visitaId);
            List<FotoVisita> fotos = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    fotos.add(new FotoVisita(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4)));
                }
            }
            return fotos;
        }
    }

    public Optional<FotoVisita> buscarFoto(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, visita_id, archivo, descripcion FROM visita_foto WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(new FotoVisita(rs.getLong(1), rs.getLong(2), rs.getString(3),
                        rs.getString(4))) : Optional.empty();
            }
        }
    }

    public long insertarFoto(Connection c, long visitaId, String archivo, String descripcion, LocalDateTime ahora)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO visita_foto (visita_id, archivo, descripcion, registrado_en) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, visitaId);
            ps.setString(2, archivo);
            ps.setString(3, descripcion);
            ps.setString(4, Fechas.aTexto(ahora));
            ps.executeUpdate();
            return clave(ps);
        }
    }

    public void eliminarFoto(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM visita_foto WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------ utilidades

    private static void asignar(PreparedStatement ps, DatosVisita d) throws SQLException {
        ps.setString(1, Fechas.fechaATexto(d.fecha()));
        ps.setString(2, d.tipo().name());
        ps.setString(3, d.inspector());
        ps.setString(4, d.lugar());
        decimal(ps, 5, d.latitud());
        decimal(ps, 6, d.longitud());
        ps.setString(7, d.atendidoPor());
        ps.setString(8, d.observaciones());
        ps.setString(9, d.conclusion().name());
        ps.setString(10, Fechas.fechaATexto(d.proximaVisita()));
    }

    private static void decimal(PreparedStatement ps, int indice, Double valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.REAL);
        } else {
            ps.setDouble(indice, valor);
        }
    }

    private static void guardarAspectos(Connection c, long visitaId, DatosVisita d) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO visita_aspecto (visita_id, aspecto, resultado, observacion) VALUES (?, ?, ?, ?)")) {
            for (Map.Entry<AspectoInspeccion, ResultadoAspecto> e : d.aspectos().entrySet()) {
                ps.setLong(1, visitaId);
                ps.setString(2, e.getKey().name());
                ps.setString(3, e.getValue().name());
                ps.setString(4, d.notas().get(e.getKey()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static long clave(PreparedStatement ps) throws SQLException {
        try (ResultSet claves = ps.getGeneratedKeys()) {
            if (!claves.next()) {
                throw new DataAccessException("No se obtuvo el identificador del registro creado");
            }
            return claves.getLong(1);
        }
    }

    private static Double leerDecimal(ResultSet rs, String columna) throws SQLException {
        double valor = rs.getDouble(columna);
        return rs.wasNull() ? null : valor;
    }

    private static List<Visita> mapear(Connection c, PreparedStatement ps) throws SQLException {
        List<Visita> visitas = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long id = rs.getLong("id");
                visitas.add(new Visita(id, rs.getLong("empresa_id"), Fechas.fechaDesdeTexto(rs.getString("fecha")),
                        TipoVisita.valueOf(rs.getString("tipo")), rs.getString("inspector"), rs.getString("lugar"),
                        leerDecimal(rs, "latitud"), leerDecimal(rs, "longitud"), rs.getString("atendido_por"),
                        rs.getString("observaciones"), ConclusionVisita.valueOf(rs.getString("conclusion")),
                        Fechas.fechaDesdeTexto(rs.getString("proxima_visita")), "ANULADA".equals(rs.getString("estado")),
                        rs.getString("motivo_anulacion"), rs.getString("registrado_por"), Map.of(), Map.of()));
            }
        }
        List<Visita> completas = new ArrayList<>();
        for (Visita v : visitas) {
            Map<AspectoInspeccion, ResultadoAspecto> aspectos = new EnumMap<>(AspectoInspeccion.class);
            Map<AspectoInspeccion, String> notas = new EnumMap<>(AspectoInspeccion.class);
            try (PreparedStatement pa = c.prepareStatement(
                    "SELECT aspecto, resultado, observacion FROM visita_aspecto WHERE visita_id = ?")) {
                pa.setLong(1, v.id());
                try (ResultSet ra = pa.executeQuery()) {
                    while (ra.next()) {
                        AspectoInspeccion a = AspectoInspeccion.valueOf(ra.getString(1));
                        aspectos.put(a, ResultadoAspecto.valueOf(ra.getString(2)));
                        if (ra.getString(3) != null) {
                            notas.put(a, ra.getString(3));
                        }
                    }
                }
            }
            completas.add(new Visita(v.id(), v.empresaId(), v.fecha(), v.tipo(), v.inspector(), v.lugar(), v.latitud(),
                    v.longitud(), v.atendidoPor(), v.observaciones(), v.conclusion(), v.proximaVisita(), v.anulada(),
                    v.motivoAnulacion(), v.registradoPor(), aspectos, notas));
        }
        return completas;
    }

    private static List<Compromiso> mapearCompromisos(PreparedStatement ps) throws SQLException {
        List<Compromiso> lista = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long verif = rs.getLong("visita_verificacion_id");
                Long verificacion = rs.wasNull() ? null : verif;
                lista.add(new Compromiso(rs.getLong("id"), rs.getLong("empresa_id"), rs.getLong("visita_id"),
                        Fechas.fechaDesdeTexto(rs.getString("fecha_visita")), rs.getString("descripcion"),
                        Fechas.fechaDesdeTexto(rs.getString("plazo")), EstadoCompromiso.valueOf(rs.getString("estado")),
                        Fechas.fechaDesdeTexto(rs.getString("fecha_verificacion")), verificacion,
                        rs.getString("observacion"), rs.getString("verificado_por")));
            }
        }
        return lista;
    }
}
