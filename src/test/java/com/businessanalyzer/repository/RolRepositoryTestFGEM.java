package com.businessanalyzer.repository;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.RolFGEM;
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

class RolRepositoryTestFGEM {

    @TempDir
    Path carpetaFGEM;

    @Test
    void losPermisosDelEsquemaCoincidenConElEnumFGEM() {
        EntornoPruebaFGEM entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        Set<String> enBaseDatosFGEM = entornoFGEM.databaseFGEM.conConexionFGEM(cFGEM -> {
            Set<String> codigosFGEM = new HashSet<>();
            try (PreparedStatement psFGEM = cFGEM.prepareStatement("SELECT codigo FROM permiso");
                 ResultSet rsFGEM = psFGEM.executeQuery()) {
                while (rsFGEM.next()) {
                    codigosFGEM.add(rsFGEM.getString(1));
                }
            }
            return codigosFGEM;
        });
        Set<String> enCodigoFGEM = EnumSet.allOf(PermisoFGEM.class).stream().map(Enum::name).collect(Collectors.toSet());

        assertEquals(enCodigoFGEM, enBaseDatosFGEM);
    }

    @Test
    void cadaRolTieneLosPermisosEsperadosFGEM() {
        EntornoPruebaFGEM entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        Map<String, RolFGEM> rolesFGEM = entornoFGEM.databaseFGEM.conConexionFGEM(entornoFGEM.rolesFGEM::listarFGEM).stream()
                .collect(Collectors.toMap(RolFGEM::codigoFGEM, rFGEM -> rFGEM));

        assertEquals(Set.of("ADMINISTRADOR", "GERENTE", "ANALISTA", "OPERADOR"), rolesFGEM.keySet());
        assertEquals(EnumSet.allOf(PermisoFGEM.class), rolesFGEM.get("ADMINISTRADOR").permisosFGEM());

        RolFGEM gerenteFGEM = rolesFGEM.get("GERENTE");
        assertFalse(gerenteFGEM.tieneFGEM(PermisoFGEM.USUARIOS_GESTIONAR));
        assertTrue(gerenteFGEM.tieneFGEM(PermisoFGEM.ANALISIS_VER));

        RolFGEM analistaFGEM = rolesFGEM.get("ANALISTA");
        for (PermisoFGEM pFGEM : List.of(PermisoFGEM.EMPRESAS_GESTIONAR, PermisoFGEM.PRODUCTOS_GESTIONAR,
                PermisoFGEM.OPERACIONES_GESTIONAR, PermisoFGEM.FINANZAS_GESTIONAR)) {
            assertFalse(analistaFGEM.tieneFGEM(pFGEM), "El analista no modifica datos: " + pFGEM);
        }

        RolFGEM operadorFGEM = rolesFGEM.get("OPERADOR");
        assertTrue(operadorFGEM.tieneFGEM(PermisoFGEM.OPERACIONES_GESTIONAR));
        assertFalse(operadorFGEM.tieneFGEM(PermisoFGEM.ANALISIS_VER));

        rolesFGEM.values().forEach(rFGEM -> assertTrue(rFGEM.tieneFGEM(PermisoFGEM.DASHBOARD_VER), "Todos ven el dashboard"));
    }
}
