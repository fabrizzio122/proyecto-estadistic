package com.businessanalyzer.repository;

import com.businessanalyzer.exception.DataAccessExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.DatosEmpresaFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.SectorFGEM;
import com.businessanalyzer.model.TamanoEmpresaFGEM;
import com.businessanalyzer.util.FechasFGEM;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Acceso a las tablas {@code empresa} y {@code usuario_empresa}. */
public class EmpresaRepositoryFGEM {

    private static final String COLUMNAS_FGEM = """
            e.id, e.razon_social, e.nombre_comercial, e.identificacion_fiscal, e.sector, e.tamano,
            e.moneda, e.mes_inicio_ejercicio, e.fecha_inicio_operaciones, e.direccion, e.telefono,
            e.email, e.activa, e.creado_en, e.actualizado_en""";

    private static final String ORDEN_FGEM = " ORDER BY COALESCE(e.nombre_comercial, e.razon_social) COLLATE NOCASE";

    /** Todas las empresas (para administradores). */
    public List<EmpresaFGEM> listarFGEM(Connection cFGEM, boolean incluirArchivadasFGEM) throws SQLException {
        String sqlFGEM = "SELECT " + COLUMNAS_FGEM + " FROM empresa e"
                + (incluirArchivadasFGEM ? "" : " WHERE e.activa = 1") + ORDEN_FGEM;
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(sqlFGEM)) {
            return mapearListaFGEM(psFGEM);
        }
    }

    /** Empresas asignadas a un usuario. */
    public List<EmpresaFGEM> listarAsignadasFGEM(Connection cFGEM, long usuarioIdFGEM, boolean incluirArchivadasFGEM) throws SQLException {
        String sqlFGEM = "SELECT " + COLUMNAS_FGEM + " FROM empresa e"
                + " JOIN usuario_empresa ue ON ue.empresa_id = e.id"
                + " WHERE ue.usuario_id = ?"
                + (incluirArchivadasFGEM ? "" : " AND e.activa = 1") + ORDEN_FGEM;
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(sqlFGEM)) {
            psFGEM.setLong(1, usuarioIdFGEM);
            return mapearListaFGEM(psFGEM);
        }
    }

    public Optional<EmpresaFGEM> buscarPorIdFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("SELECT " + COLUMNAS_FGEM + " FROM empresa e WHERE e.id = ?")) {
            psFGEM.setLong(1, idFGEM);
            List<EmpresaFGEM> resultadoFGEM = mapearListaFGEM(psFGEM);
            return resultadoFGEM.isEmpty() ? Optional.empty() : Optional.of(resultadoFGEM.get(0));
        }
    }

    /** Comparación sin distinguir mayúsculas (columna COLLATE NOCASE). */
    public boolean existeIdentificacionFGEM(Connection cFGEM, String identificacionFGEM, long excluirIdFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "SELECT 1 FROM empresa WHERE identificacion_fiscal = ? AND id <> ?")) {
            psFGEM.setString(1, identificacionFGEM);
            psFGEM.setLong(2, excluirIdFGEM);
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                return rsFGEM.next();
            }
        }
    }

    public long insertarFGEM(Connection cFGEM, DatosEmpresaFGEM dFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                INSERT INTO empresa (razon_social, nombre_comercial, identificacion_fiscal, sector, tamano, moneda,
                                     mes_inicio_ejercicio, fecha_inicio_operaciones, direccion, telefono, email,
                                     creado_en, actualizado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            asignarDatosFGEM(psFGEM, dFGEM);
            psFGEM.setString(12, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setString(13, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.executeUpdate();
            try (ResultSet clavesFGEM = psFGEM.getGeneratedKeys()) {
                if (!clavesFGEM.next()) {
                    throw new DataAccessExceptionFGEM("No se obtuvo el identificador de la empresa creada");
                }
                return clavesFGEM.getLong(1);
            }
        }
    }

    public void actualizarFGEM(Connection cFGEM, long idFGEM, DatosEmpresaFGEM dFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                UPDATE empresa
                SET razon_social = ?, nombre_comercial = ?, identificacion_fiscal = ?, sector = ?, tamano = ?,
                    moneda = ?, mes_inicio_ejercicio = ?, fecha_inicio_operaciones = ?, direccion = ?,
                    telefono = ?, email = ?, actualizado_en = ?
                WHERE id = ?""")) {
            asignarDatosFGEM(psFGEM, dFGEM);
            psFGEM.setString(12, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(13, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate());
        }
    }

    public void cambiarActivaFGEM(Connection cFGEM, long idFGEM, boolean activaFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "UPDATE empresa SET activa = ?, actualizado_en = ? WHERE id = ?")) {
            psFGEM.setInt(1, activaFGEM ? 1 : 0);
            psFGEM.setString(2, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(3, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate());
        }
    }

    // ------------------------------------------------------------------ acceso de usuarios

    public boolean estaAsignadaFGEM(Connection cFGEM, long usuarioIdFGEM, long empresaIdFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "SELECT 1 FROM usuario_empresa WHERE usuario_id = ? AND empresa_id = ?")) {
            psFGEM.setLong(1, usuarioIdFGEM);
            psFGEM.setLong(2, empresaIdFGEM);
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                return rsFGEM.next();
            }
        }
    }

    public Set<Long> usuariosAsignadosFGEM(Connection cFGEM, long empresaIdFGEM) throws SQLException {
        Set<Long> idsFGEM = new LinkedHashSet<>();
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "SELECT usuario_id FROM usuario_empresa WHERE empresa_id = ?")) {
            psFGEM.setLong(1, empresaIdFGEM);
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                while (rsFGEM.next()) {
                    idsFGEM.add(rsFGEM.getLong(1));
                }
            }
        }
        return idsFGEM;
    }

    public void asignarFGEM(Connection cFGEM, long usuarioIdFGEM, long empresaIdFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "INSERT OR IGNORE INTO usuario_empresa (usuario_id, empresa_id) VALUES (?, ?)")) {
            psFGEM.setLong(1, usuarioIdFGEM);
            psFGEM.setLong(2, empresaIdFGEM);
            psFGEM.executeUpdate();
        }
    }

    /** Deja exactamente esos usuarios asignados a la empresa. Usar dentro de una transacción. */
    public void reemplazarAsignacionesFGEM(Connection cFGEM, long empresaIdFGEM, Set<Long> usuarioIdsFGEM) throws SQLException {
        try (PreparedStatement borrarFGEM = cFGEM.prepareStatement("DELETE FROM usuario_empresa WHERE empresa_id = ?")) {
            borrarFGEM.setLong(1, empresaIdFGEM);
            borrarFGEM.executeUpdate();
        }
        for (long usuarioIdFGEM : usuarioIdsFGEM) {
            asignarFGEM(cFGEM, usuarioIdFGEM, empresaIdFGEM);
        }
    }

    // ------------------------------------------------------------------ mapeo

    private static void asignarDatosFGEM(PreparedStatement psFGEM, DatosEmpresaFGEM dFGEM) throws SQLException {
        psFGEM.setString(1, dFGEM.razonSocialFGEM());
        psFGEM.setString(2, dFGEM.nombreComercialFGEM());
        psFGEM.setString(3, dFGEM.identificacionFiscalFGEM());
        psFGEM.setString(4, dFGEM.sectorFGEM().name());
        psFGEM.setString(5, dFGEM.tamanoFGEM().name());
        psFGEM.setString(6, dFGEM.monedaFGEM().name());
        psFGEM.setInt(7, dFGEM.mesInicioEjercicioFGEM());
        psFGEM.setString(8, FechasFGEM.fechaATextoFGEM(dFGEM.fechaInicioOperacionesFGEM()));
        psFGEM.setString(9, dFGEM.direccionFGEM());
        psFGEM.setString(10, dFGEM.telefonoFGEM());
        psFGEM.setString(11, dFGEM.emailFGEM());
    }

    private static List<EmpresaFGEM> mapearListaFGEM(PreparedStatement psFGEM) throws SQLException {
        List<EmpresaFGEM> empresasFGEM = new ArrayList<>();
        try (ResultSet rsFGEM = psFGEM.executeQuery()) {
            while (rsFGEM.next()) {
                empresasFGEM.add(new EmpresaFGEM(
                        rsFGEM.getLong("id"),
                        rsFGEM.getString("razon_social"),
                        rsFGEM.getString("nombre_comercial"),
                        rsFGEM.getString("identificacion_fiscal"),
                        SectorFGEM.valueOf(rsFGEM.getString("sector")),
                        TamanoEmpresaFGEM.valueOf(rsFGEM.getString("tamano")),
                        MonedaFGEM.valueOf(rsFGEM.getString("moneda")),
                        rsFGEM.getInt("mes_inicio_ejercicio"),
                        FechasFGEM.fechaDesdeTextoFGEM(rsFGEM.getString("fecha_inicio_operaciones")),
                        rsFGEM.getString("direccion"),
                        rsFGEM.getString("telefono"),
                        rsFGEM.getString("email"),
                        rsFGEM.getInt("activa") == 1,
                        FechasFGEM.desdeTextoFGEM(rsFGEM.getString("creado_en")),
                        FechasFGEM.desdeTextoFGEM(rsFGEM.getString("actualizado_en"))));
            }
        }
        return empresasFGEM;
    }

    private static void exigirUnaFilaFGEM(int filasAfectadasFGEM) {
        if (filasAfectadasFGEM != 1) {
            throw new ValidacionExceptionFGEM("La empresa ya no existe. Actualiza la lista e inténtalo de nuevo.");
        }
    }
}
