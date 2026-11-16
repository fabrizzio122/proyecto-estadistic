package com.businessanalyzer.repository;

import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.RolFGEM;

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
public class RolRepositoryFGEM {

    private static final String CONSULTA_FGEM = """
            SELECT r.id, r.codigo, r.nombre, r.descripcion, rp.permiso_codigo
            FROM rol r
            LEFT JOIN rol_permiso rp ON rp.rol_id = r.id
            ORDER BY r.id""";

    public List<RolFGEM> listarFGEM(Connection cFGEM) throws SQLException {
        Map<Long, FilaRolFGEM> filasFGEM = new LinkedHashMap<>();
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(CONSULTA_FGEM);
             ResultSet rsFGEM = psFGEM.executeQuery()) {
            while (rsFGEM.next()) {
                long idFGEM = rsFGEM.getLong("id");
                FilaRolFGEM filaFGEM = filasFGEM.get(idFGEM);
                if (filaFGEM == null) {
                    filaFGEM = new FilaRolFGEM(idFGEM, rsFGEM.getString("codigo"), rsFGEM.getString("nombre"), rsFGEM.getString("descripcion"));
                    filasFGEM.put(idFGEM, filaFGEM);
                }
                String codigoPermisoFGEM = rsFGEM.getString("permiso_codigo");
                if (codigoPermisoFGEM != null) {
                    permisoDesdeCodigoFGEM(codigoPermisoFGEM).ifPresent(filaFGEM.permisosFGEM::add);
                }
            }
        }
        List<RolFGEM> rolesFGEM = new ArrayList<>();
        for (FilaRolFGEM filaFGEM : filasFGEM.values()) {
            rolesFGEM.add(new RolFGEM(filaFGEM.idFGEM, filaFGEM.codigoFGEM, filaFGEM.nombreFGEM, filaFGEM.descripcionFGEM, filaFGEM.permisosFGEM));
        }
        return rolesFGEM;
    }

    public Optional<RolFGEM> buscarPorIdFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        return listarFGEM(cFGEM).stream().filter(rFGEM -> rFGEM.idFGEM() == idFGEM).findFirst();
    }

    public Optional<RolFGEM> buscarPorCodigoFGEM(Connection cFGEM, String codigoFGEM) throws SQLException {
        return listarFGEM(cFGEM).stream().filter(rFGEM -> rFGEM.codigoFGEM().equals(codigoFGEM)).findFirst();
    }

    private static Optional<PermisoFGEM> permisoDesdeCodigoFGEM(String codigoFGEM) {
        return Arrays.stream(PermisoFGEM.values()).filter(pFGEM -> pFGEM.name().equals(codigoFGEM)).findFirst();
    }

    private static final class FilaRolFGEM {
        private final long idFGEM;
        private final String codigoFGEM;
        private final String nombreFGEM;
        private final String descripcionFGEM;
        private final EnumSet<PermisoFGEM> permisosFGEM = EnumSet.noneOf(PermisoFGEM.class);

        private FilaRolFGEM(long idFGEM, String codigoFGEM, String nombreFGEM, String descripcionFGEM) {
            this.idFGEM = idFGEM;
            this.codigoFGEM = codigoFGEM;
            this.nombreFGEM = nombreFGEM;
            this.descripcionFGEM = descripcionFGEM;
        }
    }
}
