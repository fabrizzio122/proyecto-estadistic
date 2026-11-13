package com.businessanalyzer.repository;

import com.businessanalyzer.model.EventoAcceso;
import com.businessanalyzer.model.RegistroAcceso;
import com.businessanalyzer.util.Fechas;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Bitácora de eventos de seguridad (inicios de sesión, bloqueos, cambios de cuentas). */
public class RegistroAccesoRepository {

    private static final int LONGITUD_MAXIMA_NOMBRE = 50;

    public void registrar(Connection c, Long usuarioId, String nombreUsuario, EventoAcceso evento,
                          String realizadoPor, LocalDateTime fecha) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO registro_acceso (usuario_id, nombre_usuario, evento, realizado_por, fecha)
                VALUES (?, ?, ?, ?, ?)""")) {
            if (usuarioId == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setLong(1, usuarioId);
            }
            ps.setString(2, recortar(nombreUsuario));
            ps.setString(3, evento.name());
            ps.setString(4, realizadoPor);
            ps.setString(5, Fechas.aTexto(fecha));
            ps.executeUpdate();
        }
    }

    public List<RegistroAcceso> listarRecientes(Connection c, int limite) throws SQLException {
        List<RegistroAcceso> registros = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT id, fecha, nombre_usuario, evento, realizado_por
                FROM registro_acceso
                ORDER BY id DESC
                LIMIT ?""")) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    registros.add(new RegistroAcceso(
                            rs.getLong("id"),
                            Fechas.desdeTexto(rs.getString("fecha")),
                            rs.getString("nombre_usuario"),
                            EventoAcceso.valueOf(rs.getString("evento")),
                            rs.getString("realizado_por")));
                }
            }
        }
        return registros;
    }

    public List<EventoAcceso> eventosDe(Connection c, String nombreUsuario) throws SQLException {
        List<EventoAcceso> eventos = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT evento FROM registro_acceso WHERE nombre_usuario = ? ORDER BY id")) {
            ps.setString(1, nombreUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    eventos.add(EventoAcceso.valueOf(rs.getString(1)));
                }
            }
        }
        return eventos;
    }

    private static String recortar(String texto) {
        String valor = texto == null ? "" : texto.strip();
        return valor.length() > LONGITUD_MAXIMA_NOMBRE ? valor.substring(0, LONGITUD_MAXIMA_NOMBRE) : valor;
    }
}
