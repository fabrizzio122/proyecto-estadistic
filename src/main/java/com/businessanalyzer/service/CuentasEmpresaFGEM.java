package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CuentaDineroFGEM;
import com.businessanalyzer.repository.TesoreriaRepositoryFGEM;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Resuelve y verifica la cuenta de dinero que indica un registro (venta, gasto, pago…). La comparten
 * los servicios que mueven dinero.
 */
final class CuentasEmpresaFGEM {

    private final TesoreriaRepositoryFGEM tesoreriaFGEM;

    CuentasEmpresaFGEM(TesoreriaRepositoryFGEM tesoreriaFGEM) {
        this.tesoreriaFGEM = tesoreriaFGEM;
    }

    /** La cuenta indicada o, si no se indicó, la cuenta principal de la empresa. */
    long resolverFGEM(Connection cFGEM, long empresaIdFGEM, Long cuentaIdFGEM, Long actualFGEM) throws SQLException {
        if (cuentaIdFGEM != null) {
            return exigirFGEM(cFGEM, empresaIdFGEM, cuentaIdFGEM, actualFGEM).idFGEM();
        }
        return tesoreriaFGEM.cuentaPrincipalFGEM(cFGEM, empresaIdFGEM)
                .orElseThrow(() -> new ValidacionExceptionFGEM(
                        "La empresa no tiene cuentas activas. Registra una en «Tesorería»."))
                .idFGEM();
    }

    /** La cuenta indicada (verificada) o {@code null} si no se indicó. */
    Long opcionalFGEM(Connection cFGEM, long empresaIdFGEM, Long cuentaIdFGEM, Long actualFGEM) throws SQLException {
        return cuentaIdFGEM == null ? null : exigirFGEM(cFGEM, empresaIdFGEM, cuentaIdFGEM, actualFGEM).idFGEM();
    }

    /**
     * Verifica que la cuenta sea de la empresa y esté activa. Una cuenta desactivada se acepta solo si el
     * registro ya la usaba ({@code actual}), para poder corregir otros datos sin cambiarla.
     */
    CuentaDineroFGEM exigirFGEM(Connection cFGEM, long empresaIdFGEM, long cuentaIdFGEM, Long actualFGEM) throws SQLException {
        CuentaDineroFGEM cuentaFGEM = tesoreriaFGEM.buscarCuentaFGEM(cFGEM, cuentaIdFGEM)
                .filter(kFGEM -> kFGEM.empresaIdFGEM() == empresaIdFGEM)
                .orElseThrow(() -> new ValidacionExceptionFGEM("La cuenta no existe en la empresa activa."));
        if (!cuentaFGEM.activaFGEM() && !Objects.equals(actualFGEM, cuentaIdFGEM)) {
            throw new ValidacionExceptionFGEM("La cuenta «" + cuentaFGEM.nombreFGEM() + "» está desactivada.");
        }
        return cuentaFGEM;
    }
}
