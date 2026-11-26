package com.businessanalyzer.repository;

import com.businessanalyzer.model.EventoAccesoFGEM;
import com.businessanalyzer.model.RegistroAccesoFGEM;
import com.businessanalyzer.util.FechasFGEM;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RegistroAccesoRepositoryFGEM {

    private static final int LONGITUD_MAXIMA_NOMBRE_FGEM = 50;

    public void registrarFGEM(Connection cFGEM, Long usuarioIdFGEM, String nombreUsuarioFGEM, EventoAccesoFGEM eventoFGEM,
                          String realizadoPorFGEM, LocalDateTime fechaFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                INSERT INTO registro_acceso (usuario_id, nombre_usuario, evento, realizado_por, fecha)
                VALUES (?, ?, ?, ?, ?)""")) {
            if (usuarioIdFGEM == null) {
                psFGEM.setNull(1, Types.INTEGER);
            } else {
                psFGEM.setLong(1, usuarioIdFGEM);
            }
            psFGEM.setString(2, recortarFGEM(nombreUsuarioFGEM));
            psFGEM.setString(3, eventoFGEM.name());
            psFGEM.setString(4, realizadoPorFGEM);
            psFGEM.setString(5, FechasFGEM.aTextoFGEM(fechaFGEM));
            psFGEM.executeUpdate();
        }
    }

    public List<RegistroAccesoFGEM> listarRecientesFGEM(Connection cFGEM, int limiteFGEM) throws SQLException {
        List<RegistroAccesoFGEM> registrosFGEM = new ArrayList<>();
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                SELECT id, fecha, nombre_usuario, evento, realizado_por
                FROM registro_acceso
                ORDER BY id DESC
                LIMIT ?""")) {
            psFGEM.setInt(1, limiteFGEM);
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                while (rsFGEM.next()) {
                    registrosFGEM.add(new RegistroAccesoFGEM(
                            rsFGEM.getLong("id"),
                            FechasFGEM.desdeTextoFGEM(rsFGEM.getString("fecha")),
                            rsFGEM.getString("nombre_usuario"),
                            EventoAccesoFGEM.valueOf(rsFGEM.getString("evento")),
                            rsFGEM.getString("realizado_por")));
                }
            }
        }
        return registrosFGEM;
    }

    public List<EventoAccesoFGEM> eventosDeFGEM(Connection cFGEM, String nombreUsuarioFGEM) throws SQLException {
        List<EventoAccesoFGEM> eventosFGEM = new ArrayList<>();
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "SELECT evento FROM registro_acceso WHERE nombre_usuario = ? ORDER BY id")) {
            psFGEM.setString(1, nombreUsuarioFGEM);
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                while (rsFGEM.next()) {
                    eventosFGEM.add(EventoAccesoFGEM.valueOf(rsFGEM.getString(1)));
                }
            }
        }
        return eventosFGEM;
    }

    private static String recortarFGEM(String textoFGEM) {
        String valorFGEM = textoFGEM == null ? "" : textoFGEM.strip();
        return valorFGEM.length() > LONGITUD_MAXIMA_NOMBRE_FGEM ? valorFGEM.substring(0, LONGITUD_MAXIMA_NOMBRE_FGEM) : valorFGEM;
    }
}
