package com.businessanalyzer.repository;

import com.businessanalyzer.EntornoPrueba;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.Rol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RolRepositoryTest {

    @TempDir
    Path carpeta;

    @Test
    void losPermisosDelEsquemaCoincidenConElEnum() {
        EntornoPrueba entorno = new EntornoPrueba(carpeta);
        Set<String> enBaseDatos = entorno.database.conConexion(c -> {
            Set<String> codigos = new HashSet<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT codigo FROM permiso");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    codigos.add(rs.getString(1));
                }
            }
            return codigos;
        });
        Set<String> enCodigo = EnumSet.allOf(Permiso.class).stream().map(Enum::name).collect(Collectors.toSet());

        assertEquals(enCodigo, enBaseDatos);
    }

    @Test
    void cadaRolTieneLosPermisosEsperados() {
        EntornoPrueba entorno = new EntornoPrueba(carpeta);
        Map<String, Rol> roles = entorno.database.conConexion(entorno.roles::listar).stream()
                .collect(Collectors.toMap(Rol::codigo, r -> r));

        assertEquals(Set.of("ADMINISTRADOR", "GERENTE", "ANALISTA", "OPERADOR"), roles.keySet());
        assertEquals(EnumSet.allOf(Permiso.class), roles.get("ADMINISTRADOR").permisos());

        Rol gerente = roles.get("GERENTE");
        assertFalse(gerente.tiene(Permiso.USUARIOS_GESTIONAR));
        assertTrue(gerente.tiene(Permiso.ANALISIS_VER));

        Rol analista = roles.get("ANALISTA");
        for (Permiso p : List.of(Permiso.EMPRESAS_GESTIONAR, Permiso.PRODUCTOS_GESTIONAR,
                Permiso.OPERACIONES_GESTIONAR, Permiso.FINANZAS_GESTIONAR)) {
            assertFalse(analista.tiene(p), "El analista no modifica datos: " + p);
        }

        Rol operador = roles.get("OPERADOR");
        assertTrue(operador.tiene(Permiso.OPERACIONES_GESTIONAR));
        assertFalse(operador.tiene(Permiso.ANALISIS_VER));

        roles.values().forEach(r -> assertTrue(r.tiene(Permiso.DASHBOARD_VER), "Todos ven el dashboard"));
    }
}
