package com.businessanalyzer.repository;

import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.Rol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Acceso a roles y sus permisos.
 * <p>
 * Los repositorios desde la Fase 2 reciben la {@link Connection} como parámetro para que
 * el servicio pueda agrupar varias operaciones en una misma transacción.
 */
public class RolRepository {

    private static final String CONSULTA = """
            SELECT r.id, r.codigo, r.nombre, r.descripcion, rp.permiso_codigo
            FROM rol r
            LEFT JOIN rol_permiso rp ON rp.rol_id = r.id
            ORDER BY r.id""";

    public List<Rol> listar(Connection c) throws SQLException {
        Map<Long, FilaRol> filas = new LinkedHashMap<>();
        try (PreparedStatement ps = c.prepareStatement(CONSULTA);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long id = rs.getLong("id");
                FilaRol fila = filas.get(id);
                if (fila == null) {
                    fila = new FilaRol(id, rs.getString("codigo"), rs.getString("nombre"), rs.getString("descripcion"));
                    filas.put(id, fila);
                }
                String codigoPermiso = rs.getString("permiso_codigo");
                if (codigoPermiso != null) {
                    permisoDesdeCodigo(codigoPermiso).ifPresent(fila.permisos::add);
                }
            }
        }
        List<Rol> roles = new ArrayList<>();
        for (FilaRol fila : filas.values()) {
            roles.add(new Rol(fila.id, fila.codigo, fila.nombre, fila.descripcion, fila.permisos));
        }
        return roles;
    }

    public Optional<Rol> buscarPorId(Connection c, long id) throws SQLException {
        return listar(c).stream().filter(r -> r.id() == id).findFirst();
    }

    public Optional<Rol> buscarPorCodigo(Connection c, String codigo) throws SQLException {
        return listar(c).stream().filter(r -> r.codigo().equals(codigo)).findFirst();
    }

    private static Optional<Permiso> permisoDesdeCodigo(String codigo) {
        return Arrays.stream(Permiso.values()).filter(p -> p.name().equals(codigo)).findFirst();
    }

    private static final class FilaRol {
        private final long id;
        private final String codigo;
        private final String nombre;
        private final String descripcion;
        private final EnumSet<Permiso> permisos = EnumSet.noneOf(Permiso.class);

        private FilaRol(long id, String codigo, String nombre, String descripcion) {
            this.id = id;
            this.codigo = codigo;
            this.nombre = nombre;
            this.descripcion = descripcion;
        }
    }
}
