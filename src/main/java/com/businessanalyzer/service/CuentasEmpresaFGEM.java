package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CuentaDineroFGEM;
import com.businessanalyzer.repository.TesoreriaRepositoryFGEM;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

final class CuentasEmpresaFGEM {

    private final TesoreriaRepositoryFGEM tesoreriaFGEM;

    CuentasEmpresaFGEM(TesoreriaRepositoryFGEM tesoreriaFGEM) {
        this.tesoreriaFGEM = tesoreriaFGEM;
    }

    long resolverFGEM(Connection cFGEM, long empresaIdFGEM, Long cuentaIdFGEM, Long actualFGEM) throws SQLException {
        if (cuentaIdFGEM != null) {
            return exigirFGEM(cFGEM, empresaIdFGEM, cuentaIdFGEM, actualFGEM).idFGEM();
        }
        return tesoreriaFGEM.cuentaPrincipalFGEM(cFGEM, empresaIdFGEM)
                .orElseThrow(() -> new ValidacionExceptionFGEM(
                        "La empresa no tiene cuentas activas. Registra una en «Tesorería»."))
                .idFGEM();
    }

    Long opcionalFGEM(Connection cFGEM, long empresaIdFGEM, Long cuentaIdFGEM, Long actualFGEM) throws SQLException {
        return cuentaIdFGEM == null ? null : exigirFGEM(cFGEM, empresaIdFGEM, cuentaIdFGEM, actualFGEM).idFGEM();
    }

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
